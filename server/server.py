# server.py
import socket
import threading
import sys
import time
import firebase_admin
from firebase_admin import credentials, firestore

# --- Server Configuration ---
HOST = '0.0.0.0'    # Accept connections from any available network interface
CHAT_PORT = 7070    # TCP port for chat connections (MUST MATCH ANDROID CLIENT'S CHAT_SERVER_PORT)
DB_COLLECTION_PATH = "artifacts"
DB_APP_ID = "collegechatapp" # The same ID the Android app will use
DB_SERVER_DOC_ID = "server_info" # The document ID for the server's IP

# --- Global Server State ---
server_socket_tcp = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket_tcp.bind((HOST, CHAT_PORT))
server_socket_tcp.listen()

client_nicknames = {} # Maps client sockets to their nicknames
client_lock = threading.Lock() # Protects access to client_nicknames

# --- Initialize Firebase Admin SDK ---
try:
    # Use the service account JSON file you downloaded from Firebase
    cred = credentials.Certificate('key.json')
    firebase_admin.initialize_app(cred)
    db = firestore.client()
    print("Firebase Admin SDK initialized successfully.")
except Exception as e:
    print(f"Error initializing Firebase Admin SDK: {e}")
    sys.exit(1)

# --- Helper Function to Get Local IP Address ---
def get_local_ip():
    """
    Attempts to get the local IP address of the machine.
    This is more reliable than socket.gethostname() for finding the routable LAN IP.
    """
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # Connects to a public DNS server (doesn't send data, just initiates connection)
        # This forces the OS to choose the correct outgoing interface and its IP.
        s.connect(('8.8.8.8', 1))
        IP = s.getsockname()[0]
    except Exception:
        IP = '127.0.0.1' # Fallback to localhost if no network connectivity
    finally:
        s.close()
    return IP

# --- Firestore IP Registration Function ---
def register_ip_to_firestore():
    """
    Periodically checks the local IP and updates the Firestore database.
    """
    last_ip = None
    while True:
        try:
            current_ip = get_local_ip()
            if current_ip != last_ip:
                print(f"Server IP changed from {last_ip} to {current_ip}. Updating Firestore.")
                doc_ref = db.collection(DB_COLLECTION_PATH).document(DB_APP_ID).collection("public").document("data").collection("server_info_collection").document(DB_SERVER_DOC_ID)
                doc_ref.set({
                    'serverIp': current_ip,
                    'timestamp': firestore.SERVER_TIMESTAMP
                })
                print(f"Successfully uploaded IP {current_ip} to Firestore.")
                last_ip = current_ip
        except Exception as e:
            print(f"Error registering IP to Firestore: {e}")
        
        sys.stdout.flush()
        time.sleep(10) # Check every 10 seconds for IP changes

# --- Chat Handling Functions (same as before) ---
def broadcast(message, sender_socket=None):
    """
    Sends a message to all connected clients.
    The sender will also receive their own message back.
    Adds a newline character at the end of each message for client-side BufferedReader.readLine().
    """
    if isinstance(message, str):
        message = message.encode('utf-8')

    with client_lock:
        for client_socket_iter in list(client_nicknames.keys()):
            try:
                client_socket_iter.send(message + b'\n')
            except Exception as e:
                print(f"Error broadcasting to {client_nicknames.get(client_socket_iter, 'Unknown')}: {e}")
                remove_client(client_socket_iter)

def remove_client(client_socket):
    """
    Removes a disconnected client from the active clients list and broadcasts their departure.
    """
    with client_lock:
        if client_socket in client_nicknames:
            nickname = client_nicknames.pop(client_socket)
            try:
                client_socket.close()
            except Exception as e:
                print(f"Error closing socket for {nickname}: {e}")
            print(f"{nickname} disconnected.")
            sys.stdout.flush()
            broadcast(f"{nickname} left the chat!")

def handle_client(client_socket, client_address):
    """
    Handles the entire communication lifecycle for a single client,
    including nickname registration and chat message exchange.
    """
    current_nickname = "Unknown"
    try:
        # --- Nickname Registration Phase ---
        client_socket.send("NICK\n".encode('utf-8'))
        print(f"[{client_address}] Sent NICK prompt.")
        sys.stdout.flush()

        nickname_bytes = b''
        while True:
            byte = client_socket.recv(1)
            if not byte:
                raise ConnectionResetError("Client disconnected during nickname registration.")
            if byte == b'\n':
                break
            nickname_bytes += byte

        received_nickname = nickname_bytes.decode('utf-8').strip()

        if not received_nickname:
            client_socket.send("Error: Nickname cannot be empty.\n".encode('utf-8'))
            print(f"[{client_address}] Empty nickname received. Disconnecting.")
            sys.stdout.flush()
            remove_client(client_socket)
            return

        with client_lock:
            if received_nickname in client_nicknames.values():
                client_socket.send("Error: Nickname already taken. Please choose another.\n".encode('utf-8'))
                print(f"[{client_address}] Nickname '{received_nickname}' already taken. Disconnecting.")
                sys.stdout.flush()
                remove_client(client_socket)
                return
            client_nicknames[client_socket] = received_nickname
            current_nickname = received_nickname

        print(f"[{client_address}] Registered as: {current_nickname}")
        sys.stdout.flush()

        broadcast(f"{current_nickname} joined the chat!")
        client_socket.send("Connected to the chat!\n".encode('utf-8'))
        sys.stdout.flush()

        # --- Chat Message Exchange Phase ---
        while True:
            message_bytes = b''
            while True:
                byte = client_socket.recv(1)
                if not byte:
                    raise ConnectionResetError("Client disconnected during chat.")
                if byte == b'\n':
                    break
                message_bytes += byte

            message = message_bytes.decode('utf-8').strip()

            if message:
                print(f"[{current_nickname}] received: {message}")
                sys.stdout.flush()
                broadcast(message)

    except ConnectionResetError:
        print(f"[{client_address}] Connection reset.")
        sys.stdout.flush()
    except UnicodeDecodeError:
        print(f"[{client_address}] Sent non-UTF-8 data. Disconnecting.")
        sys.stdout.flush()
    except Exception as e:
        print(f"[{client_address}] Error handling client: {e}")
        sys.stdout.flush()
    finally:
        remove_client(client_socket)

def receive_connections():
    """
    Listens for incoming client connections (TCP) and starts a new thread for each.
    """
    print("Server is listening for chat connections on {}:{}".format(HOST, CHAT_PORT))
    sys.stdout.flush()
    while True:
        try:
            client_socket, client_address = server_socket_tcp.accept()
            print(f"Incoming chat connection from {client_address}")
            sys.stdout.flush()
            thread = threading.Thread(target=handle_client, args=(client_socket, client_address))
            thread.daemon = True
            thread.start()
        except KeyboardInterrupt:
            print("\nServer shutting down.")
            break
        except Exception as e:
            print(f"Error accepting connection: {e}")
            sys.stdout.flush()

# --- Main Execution Block ---
if __name__ == "__main__":
    # Start the Firestore IP registration in a separate thread
    ip_registration_thread = threading.Thread(target=register_ip_to_firestore)
    ip_registration_thread.daemon = True
    ip_registration_thread.start()
    
    # Start the TCP server for chat connections in the main thread
    receive_connections()

    # Close the TCP server socket when the main thread exits
    server_socket_tcp.close()
    print("TCP Server socket closed.")
    sys.stdout.flush()
