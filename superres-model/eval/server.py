import socket
import os
import time
import subprocess
import sys
from argparse import ArgumentParser

# default values
# can be changed from command line
# arguments when running
ip = '0.0.0.0'
port = 61275

# TODO: Don't bother with filesystem
# Do everything in-memory
image_file_name = 'low_res_img.png'
model_path = '../saved-model'

# argument parser
def parse_arguments():
  parser = ArgumentParser()
  parser.add_argument(
    '--bind',
    help='bind to address')
  parser.add_argument(
    '--port',
    help='bind to port')
  parser.add_argument(
    '--model',
    help='specify the model path')
  parser.add_argument(
    '--image',
    help='specify which path to save image')
  parser.add_argument(
    '--verbose', action='store_true',
    help='enable verbose mode')
  parser.add_argument(
    '--single', action='store_true',
    help='single image mode')
  args = parser.parse_args()
  return args

# SR processing
def process():
  proc = subprocess.Popen(["python", 'superresolve.py',
    model_path, image_file_name],
    stdout=subprocess.PIPE,stderr=subprocess.PIPE)
  (out, err) = proc.communicate()
  return err

def handle_request(clientSock):
  imageSize = int(clientSock.recv(10))
  if imageSize == 0:
    return True
  print('Reading ' + str(imageSize) + ' bytes of data...')

  # Get data from client
  imageData = b''
  sizetoread = imageSize
  readData = clientSock.recv(sizetoread)
  while True:
    imageData += readData
    readSize = len(imageData)
    if readSize == imageSize:
      break
    sizetoread = imageSize-readSize
    readData = clientSock.recv(sizetoread)
  print(str(len(imageData)) + ' bytes read.')

  # write data to file
  with open(image_file_name, 'wb') as f:
    f.write(imageData)

  # apply SR on file
  print('Superresolution started...')
  t = time.time()
  err = process()
  t = time.time() - t
  if err and args.verbose:
    print(err)
  print('Superresolution finished.')

  # send file back
  print('Sending SR image...')
  with open('sr-images/'+image_file_name[:-4]+'-sr.png', 'rb') as srData:
    buff = srData.read()
    buffSize = len(buff)
    buffSizeStr = str(buffSize)
    print('Sending ' + buffSizeStr + ' bytes of data')
    #srSize = '%10s' % buffSizeStr
    #clientSock.send(srSize)
    clientSock.send(buff)

  print('File sent.')
  print('Time taken: ' + str(t))

  # cleanup
  #os.system('rm -rf sr-images')
  return False

if __name__ == '__main__':
  args = parse_arguments()
  if args.bind:
    ip = args.bind
  if args.port:
    port = int(args.port)
  if args.model:
    model_path = args.model
  if args.image:
    image_file_name = args.image
  addr = (ip, port)

  sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
  sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
  sock.bind(addr)
  sock.listen(1)

  print('Listening on: ' + str(addr))

  clientSock, clientAddr = sock.accept()

  if args.single:
    handle_request(clientSock)
  else:
    completed = False
    while not completed:
      completed = handle_request(clientSock)
  print('Closing connection...')
  clientSock.close()
  sock.close()
  sys.exit()
