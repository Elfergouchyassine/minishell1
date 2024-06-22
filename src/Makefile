# Variables
SRC_DIR := .
BIN_DIR := bin
SRC_FILES := $(wildcard $(SRC_DIR)/*.java)
MAIN_CLASS := Main

# Targets
.PHONY: all clean run

all: $(BIN_DIR) $(BIN_DIR)/$(MAIN_CLASS).class

$(BIN_DIR):
	mkdir -p $(BIN_DIR)

$(BIN_DIR)/%.class: $(SRC_DIR)/%.java
	javac -d $(BIN_DIR) $(SRC_FILES)

run: all
	java -cp $(BIN_DIR) $(MAIN_CLASS)

clean:
	rm -rf $(BIN_DIR)