import socket
import os
import sys

ip = '0.0.0.0'
port = 61274
addr = (ip, port)

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.bind(addr)
sock.listen(1)

print('Listening on: ' + str(addr))

clientSock, clientAddr = sock.accept()
imageSize = int(clientSock.recv(10))
print('Reading ' + str(imageSize) + ' bytes of data')
try:
  imageData = ""
  readData = clientSock.recv(4096)
  while True:
    imageData += readData
    if len(imageData) == imageSize:
      break
    readData = clientSock.recv(4096)
  print(str(len(imageData)) + ' bytes read')
  with open('low_res_image.png', 'wb') as f:
    f.write(imageData)
  os.system('python superresolve.py ../saved-model low_res_image.png')
except:
  clientSock.close()
  sock.close()
