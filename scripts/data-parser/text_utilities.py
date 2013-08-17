import re

r"""
>>> from text_utilities import *

"""

def nice(line, stop_words = []):
    r""" 
    Coerce a line of text into a nicer format and remove stop words
    if any are provided.
    
    >>> line = 'Hello... This, this is 1 test; testing...\n'
    >>> nice(line) 
    'hello this this is test testing'
    >>> nice(line, ['this','is']) 
    'hello test testing'
    
    """
    l = line.strip()
    l = l.lower()
    w = re.findall(r'[a-zA-Z]+',l)
    if len(stop_words) > 0:
        w = remove_stop_words(w, stop_words)
    l = ' '.join(w)
    return l
    
def remove_stop_words(word_list, stop_words):
    r"""
    Remove stop words from a line.
    
    >>> word_list = ['hello','this','this','is','1','test','testing']
    >>> stop_words = ['this','is']
    >>> remove_stop_words(word_list, stop_words)
    ['hello', '1', 'test', 'testing']
    
    """
    out = []
    for w in word_list:
        if w not in stop_words:
            out += [w]
    return out
