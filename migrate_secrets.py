import os
import yaml
import requests
from huggingface_hub import HfApi

# Configuration
HF_TOKEN = os.environ.get("HF_TOKEN") or input("Enter Hugging Face Token: ")
HF_REPO = "mechreaper007x/mood-journal-backend"

RENDER_CONFIG_PATH = os.path.expanduser(r"~\.render\cli.yaml")

def get_render_token():
    if not os.path.exists(RENDER_CONFIG_PATH):
        print(f"[ERROR] Render config not found at: {RENDER_CONFIG_PATH}")
        return None
    try:
        with open(RENDER_CONFIG_PATH, "r") as f:
            config = yaml.safe_load(f)
            return config.get("api", {}).get("key")
    except Exception as e:
        print(f"[ERROR] Error reading Render config: {e}")
        return None

def main():
    token = get_render_token()
    if not token:
        print("[ERROR] Could not retrieve Render token. Please make sure you have the Render CLI installed.")
        return

    headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "application/json"
    }

    # 1. Retrieve services
    print("[INFO] Fetching services from Render...")
    url = "https://api.render.com/v1/services"
    response = requests.get(url, headers=headers)
    
    if response.status_code == 401:
        print("\n" + "!" * 80)
        print("[ERROR] Render Token is Unauthorized or Expired!")
        print("-> Please run the following command in your terminal / PowerShell to login:")
        print("   render login")
        print("Once logged in successfully, please run this migration script again.")
        print("!" * 80 + "\n")
        return
    elif response.status_code != 200:
        print(f"[ERROR] Failed to fetch services from Render: {response.status_code} - {response.text}")
        return

    services_data = response.json()
    # Support both list of dicts, or list of dicts with wrapper key 'service'
    services = []
    for item in services_data:
        if isinstance(item, dict):
            # Render API might wrap service details inside a "service" key
            s = item.get("service", item)
            services.append(s)

    backend_service = None
    for s in services:
        name = s.get("name", "").lower()
        if "backend" in name or "mood-journal" in name:
            backend_service = s
            break

    if not backend_service:
        print("[ERROR] No service with 'backend' or 'mood-journal' in its name was found on Render.")
        print("Available services:")
        for s in services:
            print(f"  - {s.get('name')} (ID: {s.get('id')})")
        return

    service_id = backend_service.get("id")
    service_name = backend_service.get("name")
    print(f"[SUCCESS] Found backend service: '{service_name}' (ID: {service_id})")

    # 2. Retrieve environment variables
    print(f"[INFO] Fetching environment variables for '{service_name}'...")
    env_url = f"https://api.render.com/v1/services/{service_id}/env-vars"
    env_response = requests.get(env_url, headers=headers)
    
    if env_response.status_code != 200:
        print(f"[ERROR] Failed to fetch environment variables: {env_response.status_code} - {env_response.text}")
        return

    env_data = env_response.json()
    secrets_to_migrate = {}
    
    for item in env_data:
        # Render API wraps env var details inside "envVar" key
        ev = item.get("envVar", item)
        key = ev.get("key")
        value = ev.get("value")
        if key and value:
            # Skip database URL, username, password as we use H2 on HF Spaces
            if any(db_key in key.lower() for db_key in ["datasource_url", "datasource_username", "datasource_password"]):
                print(f"  [INFO] Skipping database secret: {key}")
                continue
            secrets_to_migrate[key] = value

    if not secrets_to_migrate:
        print("[ERROR] No secrets found to migrate (excluding database configurations).")
        return

    print(f"[SUCCESS] Found {len(secrets_to_migrate)} secrets to migrate:")
    for k in secrets_to_migrate.keys():
        print(f"  - {k}")

    # 3. Add to Hugging Face
    print("\n" + "=" * 60)
    print("[INFO] Migrating secrets to Hugging Face Space...")
    print("=" * 60)

    hf_api = HfApi(token=HF_TOKEN)
    
    for key, value in secrets_to_migrate.items():
        try:
            print(f"[INFO] Uploading secret '{key}' to Hugging Face...")
            hf_api.add_space_secret(repo_id=HF_REPO, key=key, value=value)
            print(f"  [SUCCESS] Secret '{key}' uploaded successfully.")
        except Exception as e:
            print(f"  [ERROR] Error uploading secret '{key}': {e}")

    # 4. Restart Space to apply settings
    try:
        print(f"\n[INFO] Triggering restart for Hugging Face Space '{HF_REPO}'...")
        hf_api.restart_space(repo_id=HF_REPO)
        print("[SUCCESS] Space restart triggered successfully. The backend will start up in a few seconds with the new secrets!")
    except Exception as e:
        print(f"[ERROR] Error restarting space: {e}")

if __name__ == "__main__":
    main()
