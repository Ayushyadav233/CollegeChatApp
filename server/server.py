# server.py
import socket
import threading
import sys # Import sys for flushing print statements

host = '0.0.0.0'  # Accept connections from any LAN IP
port = 5050

server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.bind((host, port))
server.listen()

# Use dictionaries to map client sockets to their nicknames
# This is more robust than separate lists for managing clients
client_nicknames = {}
# A lock to protect access to the client_nicknames dictionary
# as it will be modified by multiple threads
client_lock = threading.Lock()

def broadcast(message, sender_socket=None):
    """
    Sends a message to all connected clients.
    The sender will also receive their own message back.
    Adds a newline character at the end of each message for client-side BufferedReader.readLine().
    """
    # Ensure message is bytes, encode if it's a string
    if isinstance(message, str):
        message = message.encode('utf-8')

    with client_lock: # Protect shared resource (client_nicknames)
        # Iterate over a copy of keys to avoid RuntimeError if dictionary changes during iteration
        for client_socket_iter in list(client_nicknames.keys()): # Renamed to avoid conflict with sender_socket
            try:
                # Send to all clients, including the sender.
                # The client-side logic will then add it to its RecyclerView.
                client_socket_iter.send(message + b'\n')
            except Exception as e:
                print(f"Error broadcasting to {client_nicknames.get(client_socket_iter, 'Unknown')}: {e}")
                # If sending fails, assume client disconnected and remove them
                remove_client(client_socket_iter)

def remove_client(client_socket):
    """
    Removes a disconnected client from the active clients list and broadcasts their departure.
    """
    with client_lock: # Protect shared resource (client_nicknames)
        if client_socket in client_nicknames:
            nickname = client_nicknames.pop(client_socket) # Remove and get nickname
            try:
                client_socket.close()
            except Exception as e:
                print(f"Error closing socket for {nickname}: {e}")
            print(f"{nickname} disconnected.")
            # Broadcast to remaining clients that this user left
            broadcast(f"{nickname} left the chat!")
        # Remove thread reference if stored (optional, but good for cleanup)
        # if client_socket in client_threads: # If you were storing threads in a dict
        #     client_threads.pop(client_socket)


def handle_client(client_socket, client_address):
    """
    Handles the entire communication lifecycle for a single client,
    including nickname registration and chat message exchange.
    """
    current_nickname = "Unknown" # Default nickname until registered
    try:
        # --- Nickname Registration Phase ---
        # 1. Server prompts client for nickname
        client_socket.send("NICK\n".encode('utf-8'))
        print(f"[{client_address}] Sent NICK prompt.")
        sys.stdout.flush() # Ensure print is flushed immediately

        # 2. Server waits to receive the nickname from the client
        # Read byte by byte until a newline is received to get the full nickname
        nickname_bytes = b''
        while True:
            byte = client_socket.recv(1)
            if not byte: # Connection closed unexpectedly
                raise ConnectionResetError("Client disconnected during nickname registration.")
            if byte == b'\n': # End of line
                break
            nickname_bytes += byte

        received_nickname = nickname_bytes.decode('utf-8').strip()

        if not received_nickname:
            client_socket.send("Error: Nickname cannot be empty.\n".encode('utf-8'))
            print(f"[{client_address}] Empty nickname received. Disconnecting.")
            sys.stdout.flush()
            remove_client(client_socket)
            return

        with client_lock: # Protect shared resource during nickname check/assignment
            if received_nickname in client_nicknames.values():
                client_socket.send("Error: Nickname already taken. Please choose another.\n".encode('utf-8'))
                print(f"[{client_address}] Nickname '{received_nickname}' already taken. Disconnecting.")
                sys.stdout.flush()
                remove_client(client_socket)
                return
            client_nicknames[client_socket] = received_nickname
            current_nickname = received_nickname # Set the actual nickname for this handler

        print(f"[{client_address}] Registered as: {current_nickname}")
        sys.stdout.flush()

        # 3. Send welcome messages
        broadcast(f"{current_nickname} joined the chat!") # Notify all clients, including new one
        client_socket.send("Connected to the chat!\n".encode('utf-8')) # Welcome message to the new client (redundant if broadcast works, but good for explicit confirmation)
        sys.stdout.flush()


        # --- Chat Message Exchange Phase ---
        while True:
            # Read incoming messages line by line
            message_bytes = b''
            while True:
                byte = client_socket.recv(1)
                if not byte: # Connection closed
                    raise ConnectionResetError("Client disconnected during chat.")
                if byte == b'\n': # End of line
                    break
                message_bytes += byte

            message = message_bytes.decode('utf-8').strip()

            if message: # Only process non-empty messages
                # Client already prepends the nickname (e.g., "Ayush: hello")
                # So, we just broadcast the received message as is.
                print(f"[{current_nickname}] received: {message}")
                sys.stdout.flush()
                broadcast(message) # Broadcast to all, including sender

    except ConnectionResetError:
        # This exception occurs when the client gracefully closes the connection
        print(f"[{client_address}] Connection reset.")
        sys.stdout.flush()
    except UnicodeDecodeError:
        print(f"[{client_address}] Sent non-UTF-8 data. Disconnecting.")
        sys.stdout.flush()
    except Exception as e:
        # Catch any other unexpected errors
        print(f"[{client_address}] Error handling client: {e}")
        sys.stdout.flush()
    finally:
        # Ensure client is removed and socket closed when handler exits
        remove_client(client_socket)


def receive_connections():
    """
    Listens for incoming client connections and starts a new thread for each.
    """
    print("Server is listening on {}:{}".format(host, port))
    sys.stdout.flush()
    while True:
        try:
            client_socket, client_address = server.accept()
            print(f"Incoming connection from {client_address}")
            sys.stdout.flush()
            # Start a new thread to handle this client
            thread = threading.Thread(target=handle_client, args=(client_socket, client_address))
            thread.daemon = True # Allow main program to exit even if threads are running
            thread.start()
        except KeyboardInterrupt:
            print("\nServer shutting down.")
            break
        except Exception as e:
            print(f"Error accepting connection: {e}")
            sys.stdout.flush()

# Start the server to receive connections
receive_connections()

# Optional: Close server socket on exit (e.g., if receive_connections loop breaks)
server.close()