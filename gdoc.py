# coding=utf-8
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import time

swagger_urls = ["http://apihost/v2/api-docs"]

# 实例化一个火狐配置文件
fp = webdriver.FirefoxProfile()

# 设置各项参数，参数可以通过在浏览器地址栏中输入about:config查看。

# 设置成0代表下载到浏览器默认下载路径；设置成2则可以保存到指定目录
fp.set_preference("browser.download.folderList", 2)

# 是否显示开始,(个人实验，不管设成True还是False，都不显示开始，直接下载)
fp.set_preference("browser.download.manager.showWhenStarting", False)

# 下载到指定目录
fp.set_preference("browser.download.dir", "D:\\doc")

# 不询问下载路径；后面的参数为要下载页面的Content-type的值
fp.set_preference("browser.helperApps.neverAsk.saveToDisk", "application/msword")

# 启动一个火狐浏览器进程，以刚才的浏览器参数
browser = webdriver.Firefox(firefox_profile=fp)

# 打开下载页面
browser.get("http://localhost:8080/")

# 下载swagger文档
try:
    for swagger_url in swagger_urls:
        pass
        # 清空, 然后输入swagger地址
        browser.find_element_by_id('swagger_url').clear()
        browser.find_element_by_id('swagger_url').send_keys(swagger_url)

        # 点击导出文档按钮
        browser.find_element_by_id("export_doc_btn").click()
        element = WebDriverWait(browser, 10).until(
            EC.presence_of_element_located((By.ID, "download_button"))
        )

        # 点击下载按钮
        browser.find_element_by_id("download_button").click()

        # 返回上一个页面
        browser.back()
finally:
    time.sleep(1)
    browser.quit()
