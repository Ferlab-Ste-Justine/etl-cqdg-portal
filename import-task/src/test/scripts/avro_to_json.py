#!/usr/bin/env python3
"""
Extract Avro test data files to JSON format for manual editing.
Creates a JSON file for each .avro file in the resources directory.
"""
import json
import os
from pathlib import Path
from fastavro import reader

# Paths
SCRIPT_DIR = Path(__file__).parent
RESOURCES_DIR = SCRIPT_DIR.parent / "resources"
JSON_DIR = SCRIPT_DIR / "json_data"

# Create JSON directory if it doesn't exist
JSON_DIR.mkdir(exist_ok=True)

def timestamp_to_string(obj):
    """Convert timestamp milliseconds to readable string format"""
    if isinstance(obj, dict):
        return {k: timestamp_to_string(v) for k, v in obj.items()}
    elif isinstance(obj, list):
        return [timestamp_to_string(item) for item in obj]
    elif isinstance(obj, int) and obj > 1000000000000:  # Likely a timestamp in millis
        # Convert to datetime string
        from datetime import datetime
        try:
            dt = datetime.fromtimestamp(obj / 1000.0)
            return dt.strftime('%Y-%m-%d %H:%M:%S.%f')[:-3] + '+00:00'
        except:
            return obj
    elif isinstance(obj, bytes):
        # Convert bytes to string representation
        return f"b'{obj.decode('latin1')}'"
    else:
        return obj

def main():
    print("="*80)
    print("AVRO TO JSON CONVERTER")
    print("="*80)
    print()
    
    # Find all .avro files
    avro_files = sorted(RESOURCES_DIR.glob("*.avro"))
    
    if not avro_files:
        print(f"No .avro files found in {RESOURCES_DIR}")
        return
    
    for avro_file in avro_files:
        json_file = JSON_DIR / avro_file.with_suffix('.json').name
        
        try:
            # Read avro file
            with open(avro_file, 'rb') as f:
                avro_reader = reader(f)
                records = list(avro_reader)
            
            # Convert timestamps and bytes to readable format
            records = [timestamp_to_string(record) for record in records]
            
            # Write to JSON file
            with open(json_file, 'w') as f:
                json.dump(records, f, indent=2, default=str)
            
            print(f"✓ {avro_file.name} -> {json_file.name} ({len(records)} records)")
            
        except Exception as e:
            print(f"✗ Error processing {avro_file.name}: {e}")
    
    print()
    print("="*80)
    print("Extraction complete! You can now edit the .json files.")
    print("="*80)

if __name__ == "__main__":
    main()
