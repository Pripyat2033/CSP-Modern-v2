import os
import time
from typing import Set, List
from urllib.parse import urljoin, urlparse
from bs4 import BeautifulSoup, Tag
import requests

start_url: str = "https://docs.fabricmc.net/develop/"
base_netloc: str = urlparse(start_url).netloc

# Explicitly typed sets and lists to satisfy Pylance
visited: Set[str] = set()
queue: List[str] = [start_url]
docs_content: List[str] = []

print("Starting to crawl Fabric documentation...")

headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) FabricDocScraper/1.0"
}

while queue:
    url: str = queue.pop(0)

    # Normalize URL by removing anchor fragments
    clean_url: str = url.split("#")[0]

    if clean_url in visited or not clean_url.startswith("https://docs.fabricmc.net/develop"):
        continue

    visited.add(clean_url)
    print(f"Crawling: {clean_url}")

    try:
        response = requests.get(clean_url, headers=headers, timeout=10)
        if response.status_code != 200:
            print(f"  -> Skipped (Status {response.status_code})")
            continue

        soup = BeautifulSoup(response.text, "html.parser")

        # Find the main container housing the documentation body
        main_content = soup.find("main") or soup.find("article") or soup.find("div", class_="content")

        if isinstance(main_content, Tag):
            # Strip out navigation sidebars, headers, and footers
            for element in main_content.select("nav, aside, header, footer, script, style"):
                element.decompose()

            text: str = main_content.get_text(separator="\n", strip=True)
            docs_content.append(
                f"\n\n==============================\n"
                f"SOURCE URL: {clean_url}\n"
                f"==============================\n\n{text}"
            )

        # Parse links recursively
        for link in soup.find_all("a", href=True):
            href_val = link.get("href")
            # Ensure href_val is a string to satisfy Pylance
            if isinstance(href_val, str):
                full_url: str = urljoin(clean_url, href_val).split("#")[0]
                if (
                    full_url.startswith("https://docs.fabricmc.net/develop")
                    and full_url not in visited
                    and full_url not in queue
                ):
                    queue.append(full_url)

        # Politeness delay between requests
        time.sleep(0.2)

    except Exception as e:
        print(f"  -> Error fetching {clean_url}: {e}")

# Save output
output_file: str = "fabric_docs_combined.txt"
with open(output_file, "w", encoding="utf-8") as f:
    _ = f.write("\n".join(docs_content))

print(
    f"\nDone! Successfully crawled {len(visited)} pages. "
    f"Saved to '{output_file}' ({os.path.getsize(output_file) / 1024:.2f} KB)"
)