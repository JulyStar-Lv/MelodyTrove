"""
Capture Figma Make content using share token URL - v2.
Use commit instead of domcontentloaded, longer waits.
"""
import asyncio
import sys
import os
import json

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
        
        console_msgs = []
        page.on("console", lambda msg: console_msgs.append(f"[{msg.type}] {msg.text}"))
        
        print(f"[1] Navigating to: {URL}", file=sys.stderr)
        try:
            await page.goto(URL, wait_until="commit", timeout=30000)
        except Exception as e:
            print(f"[goto] {e}", file=sys.stderr)
        
        # Wait for content - try multiple waits
        print("[2] Waiting 30s for app to render...", file=sys.stderr)
        await page.wait_for_timeout(30000)
        
        # Take screenshot with minimal options (no font wait)
        try:
            await page.screenshot(
                path=f"{SCREENSHOT_DIR}/figma_share_initial.png",
                full_page=False,
                timeout=15000
            )
            print("[3] Screenshot saved", file=sys.stderr)
        except Exception as e:
            print(f"[screenshot] {e}", file=sys.stderr)
        
        # Get page title and URL
        try:
            title = await page.title()
            current_url = page.url
            print(f"[4] Title: {title}", file=sys.stderr)
            print(f"[4] URL: {current_url}", file=sys.stderr)
        except:
            pass
        
        # Extract all text content
        try:
            text_content = await page.evaluate("() => document.body ? document.body.innerText : 'NO BODY'")
            with open(f"{SCREENSHOT_DIR}/figma_share_text.txt", "w") as f:
                f.write(text_content)
            print(f"[5] Text content saved ({len(text_content)} chars)", file=sys.stderr)
        except Exception as e:
            print(f"[text] {e}", file=sys.stderr)
        
        # List all frames
        try:
            frames = page.frames
            print(f"[6] Found {len(frames)} frames:", file=sys.stderr)
            for i, frame in enumerate(frames):
                print(f"    Frame {i}: {frame.url}", file=sys.stderr)
        except:
            pass
        
        # Get all hrefs
        try:
            hrefs = await page.evaluate("""() => {
                return Array.from(document.querySelectorAll('a[href]')).map(a => ({
                    href: a.href,
                    text: (a.textContent || '').trim().substring(0, 80)
                }));
            }""")
            print(f"[7] Found {len(hrefs)} links:", file=sys.stderr)
            for h in hrefs[:30]:
                print(f"    {h['text']}: {h['href']}", file=sys.stderr)
        except:
            pass
        
        # Get HTML structure (abbreviated)
        try:
            html_preview = await page.evaluate("""() => {
                const body = document.body;
                if (!body) return 'NO BODY';
                function getStructure(el, depth=0) {
                    if (depth > 3) return '';
                    let result = '';
                    for (const child of el.children) {
                        const tag = child.tagName.toLowerCase();
                        const cls = (child.className || '').toString().substring(0, 50);
                        const id = child.id || '';
                        const text = (child.textContent || '').trim().substring(0, 30);
                        result += '  '.repeat(depth) + `<${tag} id="${id}" class="${cls}">${text}\n`;
                        result += getStructure(child, depth+1);
                    }
                    return result;
                }
                return getStructure(body).substring(0, 5000);
            }""")
            with open(f"{SCREENSHOT_DIR}/figma_share_structure.txt", "w") as f:
                f.write(html_preview)
            print(f"[8] HTML structure saved", file=sys.stderr)
        except Exception as e:
            print(f"[structure] {e}", file=sys.stderr)
        
        # Save console messages
        with open(f"{SCREENSHOT_DIR}/figma_share_console.txt", "w") as f:
            f.write("\n".join(console_msgs[-50:]))
        print(f"[9] Console messages saved ({len(console_msgs)} total)", file=sys.stderr)
        
        await browser.close()

asyncio.run(main())
