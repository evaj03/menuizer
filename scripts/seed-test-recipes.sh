#!/usr/bin/env bash
set -euo pipefail

api_base_url="${API_BASE_URL:-http://localhost:8080}"
api_base_url="${api_base_url%/}"

add_recipe() {
    local title="$1"
    local type="$2"

    printf 'Creating %s recipe: %s\n' "$type" "$title"
    curl --fail-with-body --silent --show-error --include \
        --request POST "$api_base_url/api/recipes" \
        --header 'Content-Type: application/json' \
        --data "{\"title\":\"$title\",\"type\":\"$type\"}"
    printf '\n'
}

add_recipe 'Miso glazed salmon' FISH
add_recipe 'Lemon herb cod' FISH
add_recipe 'Tuna rice bowl' FISH

add_recipe 'Beef and broccoli' MEAT
add_recipe 'Roast chicken' MEAT
add_recipe 'Pork tenderloin' MEAT

add_recipe 'Mushroom risotto' VEGETABLE
add_recipe 'Roasted vegetable pasta' VEGETABLE
add_recipe 'Lentil curry' VEGETABLE