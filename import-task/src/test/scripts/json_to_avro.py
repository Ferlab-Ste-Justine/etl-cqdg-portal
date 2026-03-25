#!/usr/bin/env python3
"""
Convert edited JSON files back to Avro format.
Reads .json files and updates corresponding .avro files in the resources directory.
"""
import json
import os
import re
from pathlib import Path
from datetime import datetime
from dateutil import parser as date_parser
from fastavro import writer, reader

# Paths
SCRIPT_DIR = Path(__file__).parent
RESOURCES_DIR = SCRIPT_DIR.parent / "resources"
JSON_DIR = SCRIPT_DIR / "json_data"

def convert_data_types(obj):
    """Convert JSON data types back to Avro-compatible format"""
    if isinstance(obj, dict):
        return {k: convert_data_types(v) for k, v in obj.items()}
    elif isinstance(obj, list):
        return [convert_data_types(item) for item in obj]
    elif isinstance(obj, str):
        # Convert timestamp strings to milliseconds
        if re.match(r'\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}', obj):
            try:
                dt = date_parser.parse(obj)
                return int(dt.timestamp() * 1000)
            except:
                pass
        
        # Convert bytes strings like "b'10'" back to bytes
        if obj.startswith("b'") and obj.endswith("'"):
            try:
                bytes_content = obj[2:-1]
                return bytes_content.encode('latin1')
            except:
                pass
        
        # Convert bytes strings like 'b"10"' back to bytes  
        if obj.startswith('b"') and obj.endswith('"'):
            try:
                bytes_content = obj[2:-1]
                return bytes_content.encode('latin1')
            except:
                pass
        
        return obj
    else:
        return obj

def get_schema_from_avro(avro_file):
    """Extract schema from existing avro file"""
    with open(avro_file, 'rb') as f:
        avro_reader = reader(f)
        return avro_reader.writer_schema

def main():
    print("="*80)
    print("JSON TO AVRO CONVERTER")
    print("="*80)
    print()
    
    # Find all .json files
    if not JSON_DIR.exists():
        print(f"JSON directory not found: {JSON_DIR}")
        print("Run avro_to_json.py first to extract the data.")
        return
    
    json_files = sorted(JSON_DIR.glob("*.json"))
    
    if not json_files:
        print(f"No .json files found in {JSON_DIR}")
        return
    
    for json_file in json_files:
        avro_file = RESOURCES_DIR / json_file.with_suffix('.avro').name
        
        # Check if corresponding .avro file exists (for schema)
        if not avro_file.exists():
            print(f"⚠ Skipping {json_file.name} - no corresponding .avro file found")
            continue
        
        try:
            # Get schema from original avro file
            schema = get_schema_from_avro(avro_file)
            
            # Read JSON file
            with open(json_file, 'r') as f:
                records = json.load(f)
            
            # Convert data types
            records = [convert_data_types(record) for record in records]
            
            # Write to avro file
            with open(avro_file, 'wb') as f:
                writer(f, schema, records)
            
            print(f"✓ {json_file.name} -> {avro_file.name} ({len(records)} records)")
            
        except Exception as e:
            print(f"✗ Error processing {json_file.name}: {e}")
    
    print()
    print("="*80)
    print("Conversion complete!")
    print("="*80)

if __name__ == "__main__":
    main()
