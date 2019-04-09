import socket
import os
import sys

ip = '0.0.0.0'
port = 61274

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.bind( (ip, port) )
sock.listen(1)

clientSock, clientAddr = sock.accept()
imageSize = int(clientSock.recv(10))
print('Reading ' + str(imageSize) + ' bytes of data')
imageData = ""
readData = clientSock.recv(4096)
while True:
  imageData += readData
  if len(imageData) == imageSize:
    break
  readData = clientSock.recv(4096)
print(str(len(imageData)) + ' bytes read')
with open('tmp', 'wb') as f:
  f.write(imageData)
clientSock.close()
sock.close()
