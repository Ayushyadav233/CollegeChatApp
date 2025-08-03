# client.py
import socket
import threading

nickname = input("Enter your nickname: ")
server_ip = input("Enter server IP (ask your friend running server): ")

client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client.connect((server_ip, 5050))

def receive():
    while True:
        try:
            message = client.recv(1024).decode('utf-8')
            if message == 'NICK':
                client.send(nickname.encode('utf-8'))
            else:
                print(message)
        except:
            print("You have been disconnected!")
            client.close()
            break

def write():
    while True:
        msg = input()
        message = f"{nickname}: {msg}"
        client.send(message.encode('utf-8'))

receive_thread = threading.Thread(target=receive)
receive_thread.start()

write_thread = threading.Thread(target=write)
write_thread.start()