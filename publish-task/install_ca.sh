#!/usr/bin/env bash
set -e

add_cert() {
    local crt_file=$1
    local alias_name=$2

    if [ -s "$crt_file" ]; then
        local der_file="${crt_file%.crt}.der"
        cp "$crt_file" "/usr/local/share/ca-certificates/${crt_file##*/}"
        update-ca-certificates
        openssl x509 -in "$crt_file" -inform pem -out "$der_file" -outform der
        keytool -noprompt -importcert -trustcacerts -cacerts -alias "$alias_name" -storepass changeit -file "$der_file"
        rm "$der_file"
        rm "$crt_file"
    fi
}

# Temporary script to accommodate temporary minio internal CA

add_cert "/opt/ca_cqdg_juno_qa.crt" "cqdg-juno-es"
