import UnityPy
import psycopg2

# Load the Unity assets file
env = UnityPy.load("M:\\SteamLibrary\\steamapps\\common\\Across the Obelisk\\AcrossTheObelisk_Data\\resources.assets")

# Connect to your SQLite database (or create one)
conn = psycopg2.connect(
    dbname="ato",
    user="admin",
    password="Mama1999Thiemo",
    host="192.168.2.193",
    port="31797"
)
cursor = conn.cursor()

# Iterate through the objects in the assets file
for obj in env.objects:
    if obj.type.name == "TextAsset":
        text_asset = obj.read()
        # Check if this is the specific asset you're looking for, e.g., named "en"
        if obj.path_id == 2653:
            # Parse the content of the text asset
            lines = text_asset.m_Script.splitlines()

            # Assuming each line is formatted as "key=value"
            for line in lines:
                if "=" in line:
                    key, value = line.split("=", 1)
                    # Insert or update key-value pair in the database
                    cursor.execute("""
                    INSERT INTO description (id, value)
                    VALUES (%s, %s)
                    ON CONFLICT (id) DO UPDATE 
                    SET value = EXCLUDED.value
                    """, (key.strip(), value.strip().split('///')[0]))

# Commit changes and close the database connection
conn.commit()
conn.close()
