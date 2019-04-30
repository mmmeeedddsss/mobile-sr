import tensorflow as tf
import train_opts as OPTS

## general losses 
def create_loss_layer(input_hr_batch, output_hr_batch, img_loss_fn, 
                      discr_logits_real=None, discr_logits_fake=None):
    # add the image comparison loss
    cmp_loss = img_loss_fn(input_hr_batch, output_hr_batch)
    tf.add_to_collection(OPTS.MODEL_LOSSES, cmp_loss)
    
    # sanity check
    all_none = discr_logits_real is None and discr_logits_fake is None
    all_sth = not (discr_logits_real is None and discr_logits_fake is None)
    assert all_none or all_sth, 'GAN parameters should either all be None or should all exist!'

    # add GAN losses
    discr_exists = discr_logits_real is not None
    if discr_exists:
        # add discriminator losses
        discr_loss_real = tf.reduce_mean( # real loss
            tf.nn.sigmoid_cross_entropy_with_logits(
                logits=discr_logits_real, labels=tf.ones_like(discr_logits_real)),
            name='discr-loss-real')
        discr_loss_fake = tf.reduce_mean( # fake loss
            tf.nn.sigmoid_cross_entropy_with_logits(
                logits=discr_logits_fake, labels=tf.zeros_like(discr_logits_fake)),
            name='discr-loss-fake') 
        tf.add_to_collection(OPTS.DISCR_LOSSES, discr_loss_real)
        tf.add_to_collection(OPTS.DISCR_LOSSES, discr_loss_fake)

        # add adversarial loss
        adv_loss = tf.reduce_mean(
            tf.nn.sigmoid_cross_entropy_with_logits(
                logits=discr_logits_fake, labels=tf.ones_like(discr_logits_fake)))
        scaled_adv_loss = tf.multiply(OPTS.LOSS['adversarial_mult'], adv_loss,
                                      name='adversarial-loss')
        tf.add_to_collection(OPTS.MODEL_LOSSES, scaled_adv_loss)

    # add regularization losses
    for loss in tf.losses.get_regularization_losses(OPTS.MODEL_SCOPE):
        tf.add_to_collection(OPTS.MODEL_LOSSES, loss)
    if discr_exists:
        for loss in tf.losses.get_regularization_losses(OPTS.DISCR_SCOPE):
            tf.add_to_collection(OPTS.DISCR_LOSSES, loss)

    # print losses
    print()
    print()
    print('LOSSES:')
    print('******************************')
    print('Printing generator losses:')
    for l in tf.get_collection(OPTS.MODEL_LOSSES):
        print(l)
    if discr_exists:
        print('******************************')
        print('Printing discriminator losses:')
        for l in tf.get_collection(OPTS.DISCR_LOSSES):
            print(l)
    print('******************************')
    print()
    print()

    # sum and return the losses
    total_loss = tf.reduce_sum(tf.get_collection(OPTS.MODEL_LOSSES))
    if not discr_exists:
        return total_loss

    total_discr_loss = tf.reduce_sum(tf.get_collection(OPTS.DISCR_LOSSES))
    return total_loss, total_discr_loss

def mse_loss(input_hr_batch, output_hr_batch):
    return tf.reduce_mean(tf.squared_difference(input_hr_batch, output_hr_batch), name='mse-loss')


## regularizers
def get_regularizer():
    return tf.contrib.layers.l2_regularizer(scale=OPTS.LOSS['l2_mult'])

def get_discr_regularizer():
    return tf.contrib.layers.l2_regularizer(scale=OPTS.LOSS['discr_l2_mult'])
