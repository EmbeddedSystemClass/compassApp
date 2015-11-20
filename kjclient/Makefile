PRG = kjclient

AJTCL_BASE = ..

CFLAGS = -O0 -g -Wall -static -DAJ_MAIN -I$(AJTCL_BASE)/ajtcl/inc -I$(AJTCL_BASE)/ajtcl/target/linux
LDFLAGS = -L$(AJTCL_BASE)/ajtcl -lajtcl_st -lpthread -lcrypto

CC = gcc
$(PRG): $(PRG).o
	$(CC) $(CFLAGS) $? $(LDFLAGS) -o $@

clean:
	rm -f $(PRG) *.o

