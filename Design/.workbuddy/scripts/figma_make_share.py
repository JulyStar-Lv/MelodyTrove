"""
Capture Figma Make content using share token URL.
The ?t= parameter may grant public view access.
"""
import asyncio
import subprocess
import sys
import os
import json
import re

SCREENSHOT_DIR = "/Users/shine/CommonWork/MobileWork/TideTunes/Design/.workbuddy/screenshots"
URL = "https://www.figma.com/make/X30WgdPxOW9skIgTUCgk4b/Design-System-for-TideTunes?t=vYU1qpiT7jTjUCnT-1"

async def main():
    from playwright.async_api import async_playwright
    
    os.makedirs(SCREENSHOT_DIR, exist_ok=True)
    
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        context = await browser.new_context(
            viewport={"width": 1920, "height": 1200},
            user_agent="Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )
        page = await context.new_page()
        
        # Collect console messages
        console_msgs = []
        page.on("console", lambda msg: console_msgs.append(f"[{msg.type}] {msg.text}"))
        
        # Collect network requests
        iframe_urls = []
        page.on("frameattached", lambda frame: iframe_urls.append(frame.url) if frame.url else None)
        
        print(f"[1] Navigating to: {URL}", file=sys.stderr)
        try:
            await page.goto(URL, wait_until="domcontentloaded", timeout=60000)
        except Exception as e:
            print(f"[goto] {e}", file=sys.stderr)
        
        # Wait for content to load
        print("[2] Waiting 20s for app to render...", file=sys.stderr)
        await page.wait_for_timeout(20000)
        
        # Take initial screenshot
        await page.screenshot(path=f"{SCREENSHOT_DIR}/figma_share_initial.png", full_page=True)
        print("[3] Initial screenshot saved", file=sys.stderr)
        
        # Get page title and URL
        title = await page.title()
        current_url = page.url
        print(f"[4] Title: {title}", file=sys.stderr)
        print(f"[4] URL: {current_url}", file=sys.stderr)
        
        # Extract all text content
        text_content = await page.evaluate("() => document.body.innerText")
        with open(f"{SCREENSHOT_DIR}/figma_share_text.txt", "w") as f:
            f.write(text_content)
        print(f"[5] Text content saved ({len(text_content)} chars)", file=sys.stderr)
        
        # List all iframes
        frames = page.frames
        print(f"[6] Found {len(frames)} frames:", file=sys.stderr)
        for i, frame in enumerate(frames):
            print(f"    Frame {i}: {frame.url}", file=sys.stderr)
        
        # Collect all iframe URLs from network
        print(f"[7] Network iframe URLs: {len(iframe_urls)}", file=sys.stderr)
        for u in iframe_urls:
            if u and 'figma' in u:
                print(f"    {u}", file=sys.stderr)
        
        # Try to find and click on navigation items in the sidebar
        # Look for sidebar links like Foundation, Components, Patterns, App Pages, etc.
        nav_items = await page.evaluate("""() => {
            const items = [];
            // Look for clickable elements with text
            const allElements = document.querySelectorAll('*');
            for (const el of allElements) {
                const text = el.textContent?.trim();
                if (text && text.length < 50 && text.length > 2) {
                    const rect = el.getBoundingClientRect();
                    if (rect.x < 300 && rect.width > 20 && rect.height > 10) {
                        // Check if it's in the sidebar area (left 300px)
                        if (['Foundation', 'Components', 'Patterns', 'App Pages', 'Home', 'Search', 'Library', 'Settings', 'Layout Behavior'].some(s => text.includes(s))) {
                            items.push({
                                text: text,
                                x: rect.x + rect.width/2,
                                y: rect.y + rect.height/2,
                                tag: el.tagName
                            });
                        }
                    }
                }
            }
            return items;
        }""")
        print(f"[8] Found {len(nav_items)} nav items:", file=sys.stderr)
        for item in nav_items[:20]:
            print(f"    {item['tag']}: '{item['text']}' at ({item['x']:.0f}, {item['y']:.0f})", file=sys.stderr)
        
        # Save nav items for reference
        with open(f"{SCREENSHOT_DIR}/figma_share_nav.json", "w") as f:
            json.dump(nav_items, f, indent=2)
        
        # Get all hrefs on the page
        hrefs = await page.evaluate("""() => {
            return Array.from(document.querySelectorAll('a[href]')).map(a => ({
                href: a.href,
                text: a.textContent?.trim()?.substring(0, 50)
            }));
        }""")
        print(f"[9] Found {len(hrefs)} links:", file=sys.stderr)
        for h in hrefs[:20]:
            print(f"    {h['text']}: {h['href']}", file=sys.stderr)
        
        # Save console messages
        with open(f"{SCREENSHOT_DIR}/figma_share_console.txt", "w") as f:
            f.write("\n".join(console_msgs))
        
        await browser.close()

asyncio.run(main())
