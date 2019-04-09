import socket
import os
import subprocess
import sys
from argparse import ArgumentParser

ip = '0.0.0.0'
port = 61274

def parse_arguments():
  parser = ArgumentParser()
  parser.add_argument(
    '--bind',
    help='bind to address')
  parser.add_argument(
    '--port',
    help='bind to port')
  parser.add_argument(
    '--verbose', action='store_true',
    help='enable verbose mode')
  args = parser.parse_args()
  return args

def process():
  proc = subprocess.Popen(["python", 'superresolve.py',
    '../saved-model', 'low_res_image.png'],
    stdout=subprocess.PIPE,stderr=subprocess.PIPE)
  (out, err) = proc.communicate()
  return err

if __name__ == '__main__':
  args = parse_arguments()
  if args.bind:
    ip = args.bind
  if args.port:
    port = int(args.port)
  addr = (ip, port)

  sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
  sock.bind(addr)
  sock.listen(1)

  print('Listening on: ' + str(addr))

  try:
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
    with open('low_res_image.png', 'wb') as f:
      f.write(imageData)
    err = process()
    if err and args.verbose:
      print(err)
    with open('sr-images/low_res_image-sr.png', 'rb') as srData:
      buff = srData.read()
      srSize = '%10s' % str(len(buff))
      clientSock.send(srSize)
      clientSock.send(buff)
    os.system('rm -rf sr-images')
  finally:
    print('Closing connection...')
    clientSock.close()
    sock.close()
    sys.exit()
