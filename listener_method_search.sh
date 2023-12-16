#!/bin/bash

# Author: Jordan Doyle

# Required Parameters:
#   - File containing the list of Android set listener methods. 
#   - Directory containing the Android app source code. 
#

script=$(echo "$0" | rev | cut -d "/" -f1 | rev)

if [ $# -ne 2 ]; then
    echo "[ERROR] ($script) - Required arguments not provided."
    exit 2
fi

if [ ! -f "$1" ]; then
    echo "[ERROR] ($script) - Input file ($1) does not exist."
    exit 3
fi

if [ ! -d "$2" ]; then
    echo "[ERROR] ($script) - Input directory ($2) does not exist."
    exit 4
fi

echo "[INFO] ($script) - Searching for lines containing set listener method."
set_listeners="$(grep -n -r --include="*.java" -F -f "$1" "$2")"
echo "[INFO] ($script) - Found $(echo "$set_listeners" | wc -l | awk '{$1=$1};1') lines containing a set listener method."

function print_seperator() { # $1 is number of tabs
    printf "\t%.0s" $(seq 1 "$1")
    printf "=%.0s" $(seq 1 $((100-$(("$1"*8)))))   # tab is 8 chars
    printf "\n"
}

found=0
declaration=0
not_found=0

echo "[INFO] ($script) - Searching for lines containing view declarations..."
while read -r line; do
    print_seperator 1
    IFS=':' && line_info=("$line") && unset IFS
    line_info[2]=$(awk '{$1=$1};1' <<< "${line_info[2]}")
    printf "\tFile Name: %s\n\tLine Number: %s\n\tCode Line: %s\n" "${line_info[0]}" "${line_info[1]}" "${line_info[2]}"
    
    if [[ "${line_info[2]}" =~ ^"findViewById"* ]]; then
        # shellcheck disable=SC2001
        id="$(sed 's/^findViewById(\(.*\)).*/\1/' <<< "${line_info[2]%.*}")"
        found=$((found+1))
        printf "\tView ID: %s\n\tResult: Found view.\n" "$id"
        continue
    elif [[ "${line_info[2]}" =~ ^"public void"* ]]; then
        declaration=$((declaration+1))
        printf "\tResult: Method declaration, view does not exist.\n"
        continue
    fi

    variable_name="$(cut -f1 -d. <<< "${line_info[2]}")"
    printf "\tVariable Name: %s\n" "$variable_name"

    regex="(?:.*\s|^)$variable_name\s*=.*findViewById\((.+)\).*"
    if view_assignments=$(grep -n -r --include="*.java" -E "$regex" "$2"); then
        found=$((found+1))
        printf "\tResult: Found view.\n"
        
        while read -r assignments; do 
            print_seperator 2
            IFS=':' && view_info=("$assignments") && unset IFS
            view_info[2]=$(awk '{$1=$1};1' <<< "${view_info[2]}")
            printf "\t\tFile Name: %s\n\t\tLine Number: %s\n\t\tCode Line: %s\n" "${view_info[0]}" "${view_info[1]}" "${view_info[2]}"
            # shellcheck disable=SC2001
            id="$(sed 's/.*findViewById(\(.*\)).*/\1/' <<< "${view_info[2]}")"
            printf "\t\tView ID: %s\n" "$id"
        done <<< "$view_assignments"
    else
        not_found=$((not_found+1))
        printf "\tResult: No view found.\n"
    fi
done <<< "$set_listeners"

print_seperator 1
echo "[INFO] ($script) - $found methods with view declarations."
echo "[INFO] ($script) - $declaration method declartion, no views exist."
echo "[INFO] ($script) - $not_found methods with no view declarations."
