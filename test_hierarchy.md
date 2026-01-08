# 层级关系解析测试

## 测试场景

根据示例数据：

```
seg lvl     fieldname
 1           limit
 1          createApp:CreateApplication
 1          name
 1          age
 1          person:Person
 1          address
 1          phone
 2          cid:Child
 2          name
 2          age
 1          birth
 1          work:Work
 1          address
 1          comany
```

## 期望的层级关系

- limit (一级)
- createApp:CreateApplication (一级)
    - name (二级 - createApp的子字段)
    - age (二级 - createApp的子字段)
- person:Person (一级)
    - address (二级 - person的子字段)
    - phone (二级 - person的子字段)
    - cid:Child (二级 - person的子字段)
        - name (三级 - cid的子字段)
        - age (三级 - cid的子字段)
- birth (一级)
- work:Work (一级)
    - address (二级 - work的子字段)
    - comany (二级 - work的子字段)

## 解析规则

1. 包含冒号（:）的字段名表示容器对象
2. 容器对象后续相同 seg lvl 的简单字段是该容器的子字段
3. 遇到相同 seg lvl 的新容器对象时，前一个容器作用域结束
4. 遇到 seg lvl 更小的字段时，弹出所有 seg lvl 更大的容器
5. 简单字段遇到 seg lvl 相同的情况，如果前一字段 seg lvl 更大，则结束当前容器作用域
