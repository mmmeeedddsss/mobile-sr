DATA_LOADER = {
    'prefetch_size': 32,
    'shuffle_multiplier': 500,
}

MODEL = {
    'summarize_weights': True,
}

TRAIN = {
    'checkpoint_dir': 'ckpts',
    'checkpoint_file': 'ckpts/model.ckpt',
    'log_dir': 'train-log',
    'log_every': 25,
    'model_dir': 'saved-model',
    'print_every': 1,
    'save_every': 100,
}
