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
inp2 = tf.placeholder(tf.float32, [1, 224, 224, 3])

with tf.variable_scope('vgg19'):
    vgg = Vgg19()
    vgg.build(inp)
with tf.variable_scope('vgg19', reuse=True):
    vgg2 = Vgg19()
    vgg2.build(inp2)

tf.summary.image('i1', inp)
tf.summary.image('i2', inp2)
summary = tf.summary.merge_all()

# run the images through the network
with tf.Session() as sess:
    with tf.summary.FileWriter('log/test', sess.graph) as writer:
        o1, o2, summ = sess.run(
            [inp, inp2, summary], 
            feed_dict={inp: i1.reshape(1, 224, 224, 3), 
                       inp2: i2.reshape(1, 224, 224, 3)})
        writer.add_summary(summ, 1)

    tf.saved_model.simple_save(
        sess,
        'model',
        inputs={'i1': inp, 'i2': inp2},
        outputs={'o1': vgg.conv1_1, 'o2': vgg.conv1_1})

print(np.sum(o1 - o2))
