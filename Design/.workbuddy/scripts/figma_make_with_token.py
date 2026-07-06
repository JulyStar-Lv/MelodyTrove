#!/usr/bin/env python3
"""Persist Figma Make screenshot using a user token via X-Figma-Token header on requests."""
import asyncio
import sys
from pathlib import Path
from playwright.async_api import async_playwright

URL = "https://www.figma.com/make/X30WgdPxOW9skIgTUCgk4b/Design-System-for-TideTunes"
OUT_DIR = Path("/Users/shine/CommonWork/MobileWork/TideTunes/Design/.workbuddy/screenshots"
)
TOKEN = "figd_Q-K4Amyb6xU-kp-1XpYjpNiS1frPFa0CV_fRyYNQ"


async def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        context = await browser.new_context(
            viewport={"width": 1920, "height": 1200},
            user_agent=(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/120.0.0.0 Safari/537.36"
            ),
        )

        # Set Figma auth cookie
        await context.add_cookies(
            [
                {
                    "name": "__Host-figma.auth.csrf",
                    "value": "dummy",
                    "domain": "figma.com",
                    "path": "/",
                    "secure": True,
                    "httpOnly": True,
                },
            ]
        )

        # Intercept and inject X-Figma-Token into all figma.com requests
        async def add_token(route):
            request = route.request
            headers = dict(request.headers)
            headers["X-Figma-Token"] = TOKEN
            await route.continue_(headers=headers)

        await context.route("**/*", add_token)

        page = await context.new_page()
        try:
            await page.goto(URL, wait_until="domcontentloaded", timeout=60000)
        except Exception as e:
            print(f"[goto] {e}", file=sys.stderr)
        # Wait longer for the heavy Figma Make SPA to fully load
        await page.wait_for_timeout(25000)
        try:
            await page.wait_for_load_state("load", timeout=30000)
        except Exception as e:
            print(f"[load] {e}", file=sys.stderr)
        await page.wait_for_timeout(10000)

        # List frames
        frames = page.frames
        print(f"frames: {len(frames)}")
        for i, f in enumerate(frames):
            print(f"  [{i}] {f.url[:120]}")

        # Screenshot
        await page.screenshot(path=str(OUT_DIR / "figma_make_token_inject.png"), full_page=False)
        print("saved figma_make_token_inject.png")

        # Find preview frame
        preview_frame = None
        for f in page.frames:
            if "figmaiframepreview.figma.site" in (f.url or ""):
                preview_frame = f
                break
        if preview_frame:
            print("preview frame found")
            # Try clicking nav items inside
            for section in ["Home", "Search", "Library", "Settings", "Foundation", "Components", "Patterns"]:
                try:
                    handle = await preview_frame.query_selector(f'text="{section}"')
                    if handle:
                        await handle.click()
                        await page.wait_for_timeout(2500)
                        out = OUT_DIR / f"figma_section_{section.lower()}.png"
                        await page.screenshot(path=str(out), full_page=False)
                        print(f"  ok {section}")
                except Exception as e:
                    print(f"  [{section}] {e}")

        await browser.close()


if __name__ == "__main__":
    asyncio.run(main())
