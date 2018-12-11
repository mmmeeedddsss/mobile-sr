#! /usr/bin/python

import numpy as np
import sys
import subprocess
import psnr
import ssim
from argparse import ArgumentParser
datasets = ['Set5', 'Set14', 'BSDS100']
set5_range = (1, 6)
set14_range = (1, 15)
bsd100_range = (1, 101)

def dataset_paths(path):
  set5_path = path + '/Set5/'
  set14_path = path + '/Set14/'
  bsd100_path = path + '/BSDS100/'
  return set5_path, set14_path, bsd100_path

def cmd_for_downscale(file_path):
  new_path = file_path[:-4] + '_LR.png'
  cmd = 'convert ' + file_path + ' -scale 50% ' + new_path
  return cmd

# create images in Low-Resolution domain, takes datasets root path as arg
# and individual flags for separate datasets
# creates them by 2x downscaling
def create_low_res(path, set5_flag=True, set14_flag=True, bsd100_flag=True):
  set5_path, set14_path, bsd100_path = dataset_paths(path)
  if set5_flag:
    for i in range(*set5_range):
      subprocess.call(cmd_for_downscale(set5_path+'{}.png'.format(i)), shell=True)
  if set14_flag:
    for i in range(*set14_range):
      subprocess.call(cmd_for_downscale(set14_path+'{}.png'.format(i)), shell=True)
  if bsd100_flag:
    for i in range(*bsd100_range):
      subprocess.call(cmd_for_downscale(bsd100_path+'{}.png'.format(i)), shell=True)

def cmd_for_SR(file_path):
  cmd = 'python bilinear.py {}'.format(file_path)
  return cmd

# apply super resolution to images in the Low-Resolution domain
# takes the datasets root path as argument
# and optional flags for individual datasets
def apply_SR(path, set5_flag=True, set14_flag=True, bsd100_flag=True):
  set5_path, set14_path, bsd100_path = dataset_paths(path)
  if set5_flag:
    for i in range(*set5_range):
      subprocess.call(cmd_for_SR(set5_path+'{}_LR.png'.format(i)), shell=True)
  if set14_flag:
    for i in range(*set14_range):
      subprocess.call(cmd_for_SR(set14_path+'{}_LR.png'.format(i)), shell=True)
  if bsd100_flag:
    for i in range(*bsd100_range):
      subprocess.call(cmd_for_SR(bsd100_path+'{}_LR.png'.format(i)), shell=True)
  
# calculates psnr values given the dataset root path
# and also optional individual flags for separate datasets
# returns a list including values
def calc_psnr_values(path, mean=True, set5_flag=True, set14_flag=True, bsd100_flag=True):
  set5_path, set14_path, bsd100_path = dataset_paths(path)
  result = []
  if set5_flag:
    set5_sum = []
    for i in range(*set5_range):
      set5_sum.append(\
        psnr.calc_psnr_file_path(set5_path+'{}.png'.format(i),\
                                 set5_path+'{}_LR_interp.png'.format(i)))
    set5_mean = np.mean(set5_sum)
    result.append(set5_mean if mean else set5_sum)
  if set14_flag:
    set14_sum = []
    for i in range(*set14_range):
      set14_sum.append(\
        psnr.calc_psnr_file_path(set14_path+'{}.png'.format(i),\
                                 set14_path+'{}_LR_interp.png'.format(i)))
    set14_mean = np.mean(set14_sum)
    result.append(set14_mean if mean else set14_sum)
  if bsd100_flag:
    bsd100_sum = []
    for i in range(*bsd100_range):
      bsd100_sum.append(\
        psnr.calc_psnr_file_path(bsd100_path+'{}.png'.format(i),\
                                 bsd100_path+'{}_LR_interp.png'.format(i)))
    bsd100_mean = np.mean(bsd100_sum)
    result.append(bsd100_mean if mean else bsd100_sum)
  return result

# calculates ssim values given the dataset root path
# and also optional individual flags for separate datasets
# returns a list including values
def calc_ssim_values(path, set5_flag=True, set14_flag=True, bsd100_flag=True):
  set5_path, set14_path, bsd100_path = dataset_paths(path)
  result = []
  if set5_flag:
    set5_sum = []
    for i in range(*set5_range):
      set5_sum.append(\
        ssim.calc_ssim_file_path(set5_path+'{}.png'.format(i),\
                                 set5_path+'{}_LR_interp.png'.format(i)))
    set5_mean = np.mean(set5_sum)
    result.append(set5_mean)
  if set14_flag:
    set14_sum = []
    for i in range(*set14_range):
      set14_sum.append(\
        ssim.calc_ssim_file_path(set14_path+'{}.png'.format(i),\
                                 set14_path+'{}_LR_interp.png'.format(i)))
    set14_mean = np.mean(set14_sum)
    result.append(set14_mean)
  if bsd100_flag:
    bsd100_sum = []
    for i in range(*bsd100_range):
      bsd100_sum.append(\
        ssim.calc_ssim_file_path(bsd100_path+'{}.png'.format(i),\
                                 bsd100_path+'{}_LR_interp.png'.format(i)))
    bsd100_mean = np.mean(bsd100_sum)
    result.append(bsd100_mean)
  return result

def parse_arguments():
    # define, read and verify the command line arguments
    parser = ArgumentParser()
    parser.add_argument(
        'dataset_path',
        help='path to datasets root')
    parser.add_argument(
        '-o',
        help='save the output to file')
    parser.add_argument(
        '-m', 
        help='if specified, calculate only METRIC values(psnr/ssim)')
    parser.add_argument(
        '-d', action='store_true',
        help='show detailed output')
    parser.add_argument(
        '-v',
        help='visualization for DATASET')
    parser.add_argument(
        '-c',
        help='compare with file')
    args = parser.parse_args()
    if not args.dataset_path:
        print('error: dataset path have to be specified')
        exit(1)
    return args

if __name__ == '__main__':
  args = parse_arguments()
  #create_low_res(path)
  #apply_SR(path)

  header = '\t{}\t{}\t{}'.format('Set5', 'Set14', 'BSD100')
  if not args.m == 'ssim':
    psnr_vals = calc_psnr_values(args.dataset_path)
    psnr_line = 'PSNR:\t{:.2f}\t{:.2f}\t{:.2f}'.format(psnr_vals[0], psnr_vals[1], psnr_vals[2])

  if not args.m == 'psnr':
    ssim = calc_ssim_values(args.dataset_path)
    ssim_line = 'SSIM:\t{:.2f}\t{:.2f}\t{:.2f}'.format(ssim[0], ssim[1], ssim[2])

  if args.o:
    with open(args.o, 'w') as f:
      f.write(header + '\n')
      if not args.m == 'ssim':
        f.write(psnr_line + '\n')
      if not args.m == 'psnr':
        f.write(ssim_line + '\n')
  elif args.c:
    with open(args.c, 'r') as f:
      content = f.readlines()
      psnr = content[1].split()[1:]
      ssim = content[2].split()[1:]
      print('old:\t' + '\t'.join(psnr))
      print(psnr_line)
      print('old:\t' + '\t'.join(ssim))
      print(ssim_line)
  elif args.v:
      path = args.dataset_path
      if args.v == 'set5':
        for i in range(set5_range):
          cv2.imshow(path+'/{}.png'.format(i))
          cv2.imshow(path+'/{}_LR_interp.png'.format(i))
          cv2.waitKey()
      elif args.v == 'set14':
        for i in range(set14_range):
          cv2.imshow(path+'/{}.png'.format(i))
          cv2.imshow(path+'/{}_LR_interp.png'.format(i))
          cv2.waitKey()
      elif args.v == 'bsd100':
        for i in range(bsd100_range):
          cv2.imshow(path+'/{}.png'.format(i))
          cv2.imshow(path+'/{}_LR_interp.png'.format(i))
          cv2.waitKey()
      else:
        print('not recognized option for dataset')
  else:
    if args.d:
      print('\tSet5\tSet14\tBSD100')
      psnr_values = calc_psnr_values(args.dataset_path, mean=False)
      for i in range(*set5_range):
        print('{}\t{:.2f}\t{:.2f}\t{:.2f}'.format(i, psnr_values[0][i-1], psnr_values[1][i-1], psnr_values[2][i-1]))
    else:
      print(header)
      if not args.m == 'ssim':
        print(psnr_line)
      if not args.m == 'psnr':
        print(ssim_line)
