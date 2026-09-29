import multiprocessing
s = []
try:
    while len(s) < 3000:
        s.append(multiprocessing.Semaphore(1))
except OSError:
    pass
print(len(s))
