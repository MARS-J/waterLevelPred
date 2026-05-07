1. 在跑算法的时候，你需要先激活conda虚拟环境tm，然后运行算法代码
2. 后端用的gradle wrapper
3. 网络不通的时候，可以尝试通过本机的7897发出请求，我开了代理
4. 数据库信息本机的5432端口，数据库名为wd，用户名postgres，密码root，驱动为org.postgresql.Driver
5. 每次完成开发都要在/home/xingyu/projects/my/parttime/waterLevelPred/doc/开发进度.md文件中写入开发内容和进度，简短一些。
6. 对于项目的简介在/home/xingyu/projects/my/parttime/waterLevelPred/doc中
7. 算法部分在/home/xingyu/projects/my/parttime/waterLevelPred/algorithm中，前端部分在/home/xingyu/projects/my/parttime/waterLevelPred/project/fe中，后端在/home/xingyu/projects/my/parttime/waterLevelPred/project/server中
8. 所有需要创建的表或者需要修改的表sql写在/home/xingyu/projects/my/parttime/waterLevelPred/project/server/dbs中
9. 前端用naive ui，页面要简约美观高级，色彩克制但高级，遵守苹果的设计规范
10. 对于数据库的操作，可以使用mcp查询数据库
11. 调用大模型的方式是curl --request POST \
  --url https://api.siliconflow.cn/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer sk-zjwnvprndsvebllzydamwitiehfllgiadyspauwnnammzrkb" \
  -d '{
    "model": "Qwen/Qwen3.5-4B",
    "messages": [
      {"role": "system", "content": "你是一个有用的助手"},
      {"role": "user", "content": "你好，请介绍一下你自己"}
    ]
  }'
