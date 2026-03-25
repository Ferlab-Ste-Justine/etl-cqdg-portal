# Test Data Scripts

Python scripts for managing test data in Avro format.

## Scripts

### 1. `avro_to_json.py`
Extracts all `.avro` files from the resources directory into `.json` files for easy editing.

**Usage:**
```bash
python3 avro_to_json.py
```

This creates `.json` files in the `json_data/` folder with the same names (e.g., `cqdg-disease.avro` → `json_data/cqdg-disease.json`).

### 2. `json_to_avro.py`
Converts edited `.json` files back to `.avro` format.

**Usage:**
```bash
python3 json_to_avro.py
```

This overwrites the existing `.avro` files with the data from the `.json` files.

## Workflow

1. **Extract data:**
   ```bash
   python3 avro_to_json.py
   ```

2. **Edit the `.json` files** in the `json_data/` folder with your preferred editor

3. **Convert back to Avro:**
   ```bash
   python3 json_to_avro.py
   ```

## Requirements

- Python 3.6+
- fastavro
- python-dateutil

Install dependencies:
```bash
pip install fastavro python-dateutil
```
