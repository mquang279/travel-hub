import json
from tqdm import tqdm


def main():
    from playwright.sync_api import sync_playwright

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        page.goto("https://travelviet.net.vn/vn/travel")

        # lấy data bằng selector
        list_travel = page.locator(".item_sidebar_province")
        list_travel_count = list_travel.count()
        provinces_data = []
        for i in range(list_travel_count):
            province = list_travel.nth(i)

            link = province.locator("a").get_attribute("href")
            title = province.locator(".item_province_title").inner_text()
            travel_count = province.locator(".item_province_travel").inner_text()
            video_count = province.locator(".item_province_post_new").inner_text()
            image_div = province.locator(".item_province_image")
            # case 1: img tag
            img_tag = image_div.locator("img")
            if img_tag.count() > 0:
                image = img_tag.get_attribute("src")
            else:
                # case 2: background-image trong style
                style = image_div.get_attribute("style")
                image = None
                if style and "url(" in style:
                    image = style.split("url(")[1].split(")")[0].replace('"', "")
            provinces_data.append(
                {
                    "title": title,
                    "link": link,
                    "travel": travel_count,
                    "video": video_count,
                    "image": image,
                }
            )
        for province in tqdm(provinces_data):
            offset = 1
            while True:
                page.goto(province["link"] + "?page=" + str(offset))
                offset += 1
                items = page.locator(".item_box_content_travel")
                for i in range(items.count()):
                    item = items.nth(i)
                    travel_item_data = extract_travel_item(item)
                    province["travel_items"] = province.get("travel_items", []) + [
                        travel_item_data
                    ]
                if page.locator("ul.pagination li.disable").count() == 0:
                    break
        with open("travel_data.json", "w", encoding="utf-8") as f:
            json.dump(provinces_data, f, ensure_ascii=False, indent=4)
        browser.close()


def extract_travel_item(item):
    """Extract travel item data from a locator element"""
    # ảnh chính
    main_image = item.locator("a.thumbs img").first.get_attribute("src")

    # title
    title = item.locator(".item_travel_content_title").first.inner_text().strip()

    # mô tả
    desc = item.locator(".item_travel_content_des").first.inner_text().strip()

    # province (có thể nhiều dòng)
    provinces = item.locator(".item_travel_content_province")
    province_list = []
    for j in range(provinces.count()):
        province_list.append(provinces.nth(j).inner_text().strip())

    # views
    views = item.locator(".item_travel_content_view").first.inner_text().strip()
    # ảnh trong carousel
    sub_images = []
    imgs = item.locator(".item_list_image_travel .owl-item:not(.cloned) img")
    for j in range(imgs.count()):
        src = imgs.nth(j).get_attribute("src")
        if src:
            sub_images.append(src)

    return {
        "title": title,
        "desc": desc,
        "province": province_list,
        "views": views,
        "main_image": main_image,
        "sub_images": sub_images,
    }


if __name__ == "__main__":
    main()
