import math
import numpy as np
from skimage.measure import compare_ssim

# Calculate SSIM of two images
# @params (original_image, imitation_image)
# @ret float
#
# Formula is:
#
#               (2*mu_x*mu_y + c1)(2*sigma_xy+c2)
# SSIM = -------------------------------------------------
#        (mu_x^2 + mu_y^2 + c1)(sigma_x^2 + sigma_y^2 + c2)
def calc_ssim_numpy(org, imit):
  return compare_ssim(org, imit, full=True, multichannel=True)[0]

# Calculate SSIM of two images given their file paths
# Depends on calc_ssim_numpy
# @params (original_image_path, imitation_image_path)
# @ret float
def calc_ssim_file_path(org, imit):
  from PIL import Image
  img1  = Image.open(org)
  img2  = Image.open(imit)

  img1 = np.array(img1)
  img2 = np.array(img2)

  return calc_ssim_numpy(img1, img2)

# For batch processing, calculate SSIM for two lists of numpy arrays.
# @params (original_images_list, imitation_images_list)
# @ret list of ssim values corresponding to given args
def calc_ssim_bat_np(org_list, imit_list):
  ssim_list = []
  for i in range(len(org_list)):
    ssim = calc_ssim_numpy(org_list[i], imit_list[i])
    ssim_list.append(ssim)

  return ssim_list

# Main routine for running script
# Cmd Line args are file paths
# Print their SSIM to stdout
if __name__ == '__main__':
  import sys
  if len(sys.argv) != 3:
    print('Two image files needed. Use "python ssim.py <org_img> <other_img>"')
    sys.exit(-1)
  print(calc_ssim_file_path(sys.argv[1], sys.argv[2]))

