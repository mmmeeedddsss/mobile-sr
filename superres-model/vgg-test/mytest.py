from sys import argv

import numpy as np
import tensorflow as tf
from PIL import Image

from vgg19 import Vgg19

_, p1, p2 = argv

# load images
i1 = Image.open(p1)
i2 = Image.open(p2)

# resize them
i1 = i1.resize((224, 224), Image.ANTIALIAS)
i2 = i2.resize((224, 224), Image.ANTIALIAS)

# turn to ndarrays and scale them to be between 0-1
i1 = np.array(i1).astype('float32') / 255.0
i2 = np.array(i2).astype('float32') / 255.0

# build the network
inp = tf.placeholder(tf.float32, [1, 224, 224, 3])
vgg = Vgg19()
vgg.build(inp)

# run the images through the network
with tf.Session() as sess:
    o1 = sess.run(vgg.conv5_4, feed_dict={inp: i1.reshape(1, 224, 224, 3)})
    o2 = sess.run(vgg.conv5_4, feed_dict={inp: i2.reshape(1, 224, 224, 3)})

print(o1.shape)
print(o2.shape)

print(np.sum(o1 - o2))

