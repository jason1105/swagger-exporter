Export swagger.

### 生成文档
1. 运行应用
2. 访问http://localhost:8080

### 批量生成文档
1. [安装Python 3](https://docs.python.org/zh-cn/3/using/index.html)
1. [安装Python 模块 selenium](https://www.cnblogs.com/sandysun/p/7838113.html)
1. 修改gdoc.py第8行, 把swaggerUrl以数组的形式填写进去
1. 修改gdoc.py第22行, 指定保存目录 
1. 运行程序 `python gdoc.py`