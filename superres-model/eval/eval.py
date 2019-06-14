#! /usr/bin/python

import numpy as np
import os
import sys
import subprocess
import psnr
import ssim
from argparse import ArgumentParser
import cv2
import matplotlib.pyplot as plt

datasets = ['Set5', 'Set14', 'BSDS100']
set5_range = (1, 6)
set14_range = (1, 15)
bsd100_range = (1, 101)
extension = '-sr'
model_path = './saved-model/'
path = './'


# normalizes the path
# replaces the path variable, default = './'
# with the path in which the script is being called
def normalize_path():
  this_file = sys.argv[0]

  # find slash character
  idx_delim = len(this_file)-1
  for i in range(idx_delim, 0, -1):
    if this_file[i] == '/':
      idx_delim = i
      break

  # not found a slash character
  # current directory case
  if idx_delim == len(this_file)-1:
    path = './'
  else:
  # replace path with the directory 
  # in which the script is being called
    path = this_file[:idx_delim+1]
  return path

def cmd_for_downscale(file_path, ratio):
  scale = (1.0 / ratio) * 100
  new_path = file_path[:-4] + '_LR.png'
  cmd = 'convert ' + file_path + ' -scale {}% '.format(scale) + new_path
  return cmd

# create images in Low-Resolution domain, takes datasets root path as arg
# and individual flags for separate datasets
# creates them by 2x downscaling
def create_low_res(path, ratio, set5_flag=True, set14_flag=True, bsd100_flag=True):
  if set5_flag:
    for i in range(*set5_range):
      subprocess.call(cmd_for_downscale(path+'/Set5_{}.png'.format(i), ratio), shell=True)
  if set14_flag:
    for i in range(*set14_range):
      subprocess.call(cmd_for_downscale(path+'/Set14_{}.png'.format(i), ratio), shell=True)
  if bsd100_flag:
    for i in range(*bsd100_range):
      subprocess.call(cmd_for_downscale(path+'/BSD100_{}.png'.format(i), ratio), shell=True)

def cmd_for_SR(file_paths):
  sr_scr_path = path + 'superresolve.py'
  cmd = 'python ' + sr_scr_path + ' ' + model_path + ' ' + ' '.join(file_paths)
  return cmd

# apply super resolution to images in the Low-Resolution domain
# takes the datasets root path as argument
# and optional flags for individual datasets
def apply_SR(path, set5_flag=True, set14_flag=True, bsd100_flag=True):
  filepaths = []
  if set5_flag:
    for i in range(*set5_range):
      filepaths.append(path+'/Set5_{}_LR.png'.format(i))
  if set14_flag:
    for i in range(*set14_range):
      filepaths.append(path+'/Set14_{}_LR.png'.format(i))
  if bsd100_flag:
    for i in range(*bsd100_range):
      filepaths.append(path+'/BSD100_{}_LR.png'.format(i))
  subprocess.call(cmd_for_SR(filepaths), shell=True)
  
# calculates psnr values given the dataset root path
# and also optional individual flags for separate datasets
# returns a list including values
def calc_psnr_values(path, mean=True, set5_flag=True, set14_flag=True, bsd100_flag=True):
  result = []
  if set5_flag:
    set5_sum = []
    for i in range(*set5_range):
      set5_sum.append(\
        psnr.calc_psnr_file_path(path+'/Set5_{}.png'.format(i),\
                                 path+'/Set5_{}_LR'.format(i)+'{}.png'.format(extension)))
    set5_mean = np.mean(set5_sum)
    result.append(set5_mean if mean else set5_sum)
  if set14_flag:
    set14_sum = []
    for i in range(*set14_range):
      set14_sum.append(\
        psnr.calc_psnr_file_path(path+'/Set14_{}.png'.format(i),\
                                 path+'/Set14_{}_LR'.format(i)+'{}.png'.format(extension)))
    set14_mean = np.mean(set14_sum)
    result.append(set14_mean if mean else set14_sum)
  if bsd100_flag:
    bsd100_sum = []
    for i in range(*bsd100_range):
      bsd100_sum.append(\
        psnr.calc_psnr_file_path(path+'/BSD100_{}.png'.format(i),\
                                 path+'/BSD100_{}_LR'.format(i)+'{}.png'.format(extension)))
    bsd100_mean = np.mean(bsd100_sum)
    result.append(bsd100_mean if mean else bsd100_sum)
  return result

# calculates ssim values given the dataset root path
# and also optional individual flags for separate datasets
# returns a list including values
def calc_ssim_values(path, mean=True, set5_flag=True, set14_flag=True, bsd100_flag=True):
  result = []
  if set5_flag:
    set5_sum = []
    for i in range(*set5_range):
      set5_sum.append(\
        ssim.calc_ssim_file_path(path+'/Set5_{}.png'.format(i),\
                                 path+'/Set5_{}_LR'.format(i)+'{}.png'.format(extension)))
    set5_mean = np.mean(set5_sum)
    result.append(set5_mean if mean else set5_sum)
  if set14_flag:
    set14_sum = []
    for i in range(*set14_range):
      set14_sum.append(\
        ssim.calc_ssim_file_path(path+'/Set14_{}.png'.format(i),\
                                 path+'/Set14_{}_LR'.format(i)+'{}.png'.format(extension)))
    set14_mean = np.mean(set14_sum)
    result.append(set14_mean if mean else set14_sum)
  if bsd100_flag:
    bsd100_sum = []
    for i in range(*bsd100_range):
      bsd100_sum.append(\
        ssim.calc_ssim_file_path(path+'/BSD100_{}.png'.format(i),\
                                 path+'/BSD100_{}_LR'.format(i)+'{}.png'.format(extension)))
    bsd100_mean = np.mean(bsd100_sum)
    result.append(bsd100_mean if mean else bsd100_sum)
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
        '-s',
        help='specify saved model path, default = current dir')
    parser.add_argument(
        '-e',
        help='extension for enhanced images')
    parser.add_argument(
        '-m', 
        help='if specified, calculate only METRIC values(psnr/ssim)')
    parser.add_argument(
        '-d', action='store_true',
        help='show detailed output')
    parser.add_argument(
        '-hm', action='store_true',
        help='create heatmap')
    parser.add_argument(
        '-v',
        help='visualization for DATASET',
        choices=['set5', 'set14', 'bsd100'])
    parser.add_argument(
        '--no-low-res', action='store_true',
        help='use this if you already have LR images and don\'t want to create them again')
    parser.add_argument(
        '--no-sr', action='store_true',
        help='use this if you already have SR\'ed images and don\'t want to create them again')
    parser.add_argument(
        '-c',
        help='compare with file')
    parser.add_argument(
        '-f',
        help='superresolution factor, defaults to 2',
        type=int,
        default=2)
    args = parser.parse_args()
    if not args.dataset_path:
        print('error: dataset path have to be specified')
        exit(1)
    return args

def show_dataset(dataset_path, vset, extension, model_available=True):
    set_attr_map = {
        'set5':     ('Set5', 5), 
        'set14':    ('Set14', 14), 
        'bsd100':   ('BSD100', 100),
    }
    prefix, set_size = set_attr_map[vset]
    path = os.path.join(dataset_path, prefix)
    # create the figure, axes and titles
    fig = plt.figure()
    lr_ax = plt.subplot(1, 3, 1)
    sr_ax = plt.subplot(1, 3, 2)
    hr_ax = plt.subplot(1, 3, 3)
    lr_ax.set_title('LR Img.')
    sr_ax.set_title('SR Img.')
    hr_ax.set_title('HR Img.')
    # remove ticks on the axes
    for ax in (lr_ax, sr_ax, hr_ax):
        ax.tick_params(
            axis='both',
            which='both',
            bottom=False,
            top=False,
            labelbottom=False,
            left=False,
            right=False,
            labelleft=False)
    # create a nonlocal counter
    i = 0 
    # callback function for matplotlib to plot with
    def plot_img(event): 
        # move on left and right keys
        nonlocal i
        if event:
            if event.key == 'right':
                i = (i + 1) % set_size
            elif event.key == 'left':
                i = (i - 1) % set_size
        j = i + 1 # an extra index to bring the range to 1-indexed
        # plot the current images
        # have to convert to RGB before plotting since we used cv2
        lr_bgr = cv2.imread(path + '_{}_LR.png'.format(j), cv2.IMREAD_COLOR)

        print(lr_bgr.shape)
        lr_ax.imshow(cv2.cvtColor(lr_bgr, cv2.COLOR_BGR2RGB))
        if model_available:
            sr_bgr = cv2.imread(path + '_{}_LR{}.png'.format(j, extension),
                                cv2.IMREAD_COLOR)
            sr_ax.imshow(cv2.cvtColor(sr_bgr, cv2.COLOR_BGR2RGB))
        hr_bgr = cv2.imread(path + '_{}.png'.format(j),
                            cv2.IMREAD_COLOR)
        hr_ax.imshow(cv2.cvtColor(hr_bgr, cv2.COLOR_BGR2RGB))
        # redraw the axes
        fig.canvas.draw_idle()
    # plot the first image in the dataset
    plot_img(None) 
    # connect the callback to draw
    fig.canvas.mpl_connect('key_press_event', plot_img)
    plt.show() # show the plot

if __name__ == '__main__':
  # replace the path with the directory
  # in which the script is being called
  # if necessary
  path = normalize_path()
  args = parse_arguments()
  if args.s:
    model_path = args.s
  if args.e:
    extension = args.e
  if not args.no_low_res:
    create_low_res(args.dataset_path, args.f)
  if not args.no_sr:
    apply_SR(args.dataset_path)
  if not args.no_sr:
    os.system('mv sr-images/* ' + args.dataset_path + '; rmdir sr-images')

  header = '\t{}\t{}\t{}'.format('Set5', 'Set14', 'BSD100')
  if not args.m == 'ssim':
    psnr_vals = calc_psnr_values(args.dataset_path)
    psnr_line = 'PSNR:\t{:.2f}\t{:.2f}\t{:.2f}'.format(psnr_vals[0], psnr_vals[1], psnr_vals[2])

  if not args.m == 'psnr':
    ssim_vals = calc_ssim_values(args.dataset_path)
    ssim_line = 'SSIM:\t{:.2f}\t{:.2f}\t{:.2f}'.format(ssim_vals[0], ssim_vals[1], ssim_vals[2])

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
      model_available = bool(args.s)
      path = args.dataset_path
      show_dataset(path, args.v, extension, model_available)
  elif args.hm:
    set5_path, set14_path, bsd100_path = dataset_paths(args.dataset_path)
    for i in range(*set5_range):
      path=set5_path
      im1 = cv2.imread(path+'/{}.png'.format(i))
      im2 = cv2.imread(path+'/{}_LR'.format(i)+'{}.png'.format(extension))
      diff = np.sum(np.absolute(im1-im2), axis=2)
      cv2.imwrite('set5_{}_diff.png'.format(i), diff)
    for i in range(*set14_range):
      path=set14_path
      im1 = cv2.imread(path+'/{}.png'.format(i))
      im2 = cv2.imread(path+'/{}_LR'.format(i)+'{}.png'.format(extension))
      diff = np.sum(np.absolute(im1-im2), axis=2)
      cv2.imwrite('set14_{}_diff.png'.format(i), diff)
    for i in range(*bsd100_range):
      path=bsd100_path
      im1 = cv2.imread(path+'/{}.png'.format(i))
      im2 = cv2.imread(path+'/{}_LR'.format(i)+'{}.png'.format(extension))
      diff = np.sum(np.absolute(im1-im2), axis=2)
      cv2.imwrite('bsd100_{}_diff.png'.format(i), diff)
  else:
    if args.d:
      print('\tSet5\t\tSet14')
      psnr_values = calc_psnr_values(args.dataset_path, mean=False)
      ssim_values = calc_ssim_values(args.dataset_path, mean=False)
      for i in range(*set5_range):
        print('{}\t{:.2f}/{:.2f}\t{:.2f}/{:.2f}'.format(i,\
          psnr_values[0][i-1], ssim_values[0][i-1], psnr_values[1][i-1],\
          ssim_values[1][i-1]))
      for i in range(*set14_range):
        print('{}\t\t\t{:.2f}/{:.2f}'.format(i,\
          psnr_values[1][i-1], ssim_values[1][i-1]))
    else:
      print(header)
      if not args.m == 'ssim':
        print(psnr_line)
      if not args.m == 'psnr':
        print(ssim_line)
