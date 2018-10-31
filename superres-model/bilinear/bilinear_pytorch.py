import torch
import torch.nn as nn
import torch.nn.functional as F
import torchvision
from PIL import Image
import sys 

class Net(nn.Module):
  def __init__(self):
    super(Net, self).__init__()
  def forward(self, x): 
    return F.interpolate(x, scale_factor=2, mode='bilinear')
    
model = Net()
with torch.no_grad():
  out = model((torchvision.transforms.ToTensor()(Image.open(sys.argv[1]))).unsqueeze(0))
  torchvision.utils.save_image(out, 'out.png')