package cn.mario.aiplatform.service.impl;


import cn.mario.aiplatform.dto.KnowledgeBaseDTO;
import cn.mario.aiplatform.entity.KnowledgeBase;
import cn.mario.aiplatform.exception.BizException;
import cn.mario.aiplatform.mapper.KnowledgeBaseMapper;
import cn.mario.aiplatform.vo.KnowledgeBaseVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @description:
 * 这个功能的作用是：**把上传的资料按业务分类管理，并控制每个 AI 应用能使用哪些资料回答问题。** 你现在的项目已经有文档导入和知识问答，可以在现有流程上扩展。
 *
 * **1. 它是干什么用的？**
 *
 * 例如，你的平台上有两个 AI 应用：
 *
 * | 应用 | 绑定知识库 | 用途 |
 * |---|---|---|
 * | 人事助手 | 员工制度、报销规范 | 回答请假、报销等问题 |
 * | 产品客服 | 产品手册、售后政策 | 回答产品使用和退换货问题 |
 *
 * 这里的“应用”指一个配置好的 AI 助手。
 *
 * 知识库相当于一个资料集合，包含名称、描述、负责人和文档：
 *
 * - **创建**：建立“员工制度”知识库，再上传相关文件。
 * - **编辑**：修改名称、描述、负责人，不需要重新生成文档向量。
 * - **删除**：清理知识库及其文档记录、向量片段。
 * - **应用绑定**：指定应用问答时检索哪些知识库；解绑不会删除资料。
 *
 * 描述用于说明资料范围；负责人用于明确谁维护资料。**负责人字段本身不等于权限控制**，权限需要结合登录用户进行校验。
 *
 * 这也不是训练模型，而是在回答前检索相关资料，交给模型参考，即你项目正在做的 RAG。
 *
 * **2. 你的项目目前缺什么？**
 *
 * 我看了代码，目前：
 *
 * - [KnowledgeController.java](/Users/mario/study/project/ai-platform/src/main/java/cn/mario/aiplatform/controller/KnowledgeController.java) 已提供文件上传、普通问答和流式问答接口。
 * - [PdfKnowledgeServiceImpl.java](/Users/mario/study/project/ai-platform/src/main/java/cn/mario/aiplatform/service/impl/PdfKnowledgeServiceImpl.java) 已实现“检索片段 → 交给模型 → 返回答案”，但检索没有按知识库过滤。
 * - [schema.sql](/Users/mario/study/project/ai-platform/src/main/resources/schema.sql) 只有文件导入记录，按文件哈希全局去重。
 * - 当前没有应用管理、知识库实体及绑定关系。
 *
 * 因此，现在基本上是**所有资料放进一个公共资料池查询**。新增功能的核心，是给资料标明归属，并让检索遵守应用绑定范围。
 *
 * **3. 建议怎么添加？**
 *
 * 先复用 PostgreSQL、PGVector、`JdbcTemplate` 和现有解析、分块逻辑，不需要为每个知识库建立独立向量表。
 *
 * 建议的数据结构如下：
 *
 * | 表 | 主要字段 | 作用 |
 * |---|---|---|
 * | `knowledge_base` | `id`、`name`、`description`、`owner_id`、创建及更新时间 | 知识库信息 |
 * | `ai_app` | `id`、`name` | 最小的应用信息 |
 * | `app_knowledge_base` | `app_id`、`knowledge_base_id` | 应用与知识库绑定关系，组合唯一 |
 * | 现有导入记录表 | 增加 `knowledge_base_id` | 标识文件归属 |
 *
 * 这里按“一个应用可以绑定多个知识库”设计；如果明确只允许绑定一个，可以直接在应用表存 `knowledge_base_id`，省去关系表。
 *
 * 接下来修改两条现有链路：
 *
 * **上传链路**
 *
 * ```text
 * 选择知识库 → 上传文件 → 校验知识库及操作权限
 * → 解析、分块 → 每个片段添加 knowledge_base_id → 保存向量
 * ```
 *
 * 文件去重由全局 `file_hash` 改为 `(knowledge_base_id, file_hash)` 唯一，允许同一文件放入不同知识库。现有 `/file` 和 `/pdf` 两个上传入口都要处理归属。
 *
 * **问答链路**
 *
 * ```text
 * 用户向指定应用提问
 * → 后端查询应用绑定的知识库
 * → 在这些知识库范围内检索
 * → 将检索片段交给模型
 * → 返回答案及来源
 * ```
 *
 * 最关键的是：**在向量检索时加入知识库过滤条件**，普通问答和流式问答都要使用同样的规则。范围由后端根据应用绑定和用户权限确定，不能直接信任前端传来的知识库 ID；未绑定知识库时，不应回退为全库检索。
 *
 * 建议提供这些接口：
 *
 * | 接口 | 功能 |
 * |---|---|
 * | `POST /knowledge-bases` | 创建知识库 |
 * | `GET /knowledge-bases` | 查询知识库列表 |
 * | `GET /knowledge-bases/{id}` | 查询详情 |
 * | `PUT /knowledge-bases/{id}` | 修改名称、描述、负责人 |
 * | `DELETE /knowledge-bases/{id}` | 删除知识库 |
 * | `POST /knowledge-bases/{id}/files` | 向指定知识库上传文件 |
 * | `PUT /apps/{appId}/knowledge-bases` | 设置应用绑定的知识库 |
 * | `POST /apps/{appId}/chat` | 使用应用绑定的知识库问答 |
 *
 * **4. 落地时需要特别处理的地方**
 *
 * - **删除一致性**：第一版建议禁止删除仍被应用绑定的知识库，先解绑；实际删除时一起清理导入记录和向量，失败不能留下半删状态。
 * - **历史数据迁移**：建立“默认知识库”，将已有导入记录和向量片段统一归入其中。
 * - **上传失败重试**：目前通用文件导入先提交记录、再写向量，向量失败后会被判定为重复上传。扩展时应一起修正写入一致性。
 * - **验证隔离**：至少验证绑定 A 的应用搜不到 B、同一文件可以上传到不同库、删除后片段不可检索，以及普通与流式问答行为一致。
 *
 * 推荐顺序是：**知识库管理 → 文件归属 → 应用绑定 → 检索隔离 → 管理页面**。这样每一步都能接上你现有的问答能力。本次是结合代码说明方案，尚未修改项目。
 * @author: mario
 * @date: 9/14/26
 */
@Service
public class KnowledgeBaseServiceImpl {

    public static final long DEFAULT_ID = 1L;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;
    
    public KnowledgeBaseVO get(long id) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(id);
        if (knowledgeBase == null) {
            throw new BizException(404, "知识库不存在");
        }

        KnowledgeBaseVO vo = new KnowledgeBaseVO();
        BeanUtils.copyProperties(knowledgeBase, vo);
        return vo;
    }

    @Transactional
    public long create(KnowledgeBaseDTO dto) {
        validate(dto);

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        BeanUtils.copyProperties(dto, knowledgeBase);
        knowledgeBaseMapper.insert(knowledgeBase);
        return knowledgeBase.getId();
    }

    @Transactional
    public void update(KnowledgeBaseDTO dto) {
        validate(dto);

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        BeanUtils.copyProperties(dto, knowledgeBase);
        int updated = knowledgeBaseMapper.updateById(knowledgeBase);

        if (updated == 0) {
            throw new BizException(404, "知识库不存在");
        }
    }

    @Transactional
    public void delete(long id) {
        if (id == DEFAULT_ID) {
            throw new BizException(409, "默认知识库不能删除");
        }

//        lock(id);
//
//        Boolean bound = jdbc.queryForObject("""
//                SELECT EXISTS (
//                    SELECT 1 FROM app_knowledge_base
//                    WHERE knowledge_base_id = ?
//                )
//                """, Boolean.class, id);
//
//        if (Boolean.TRUE.equals(bound)) {
//            throw new BizException(409, "知识库已绑定应用，请先解绑");
//        }
//
//        // 当前项目使用同一 PostgreSQL 数据源，三步在同一事务中提交。
//        jdbc.update("""
//                DELETE FROM vector_store
//                WHERE metadata ->> 'knowledge_base_id' = ?
//                """, Long.toString(id));
//        jdbc.update("""
//                DELETE FROM knowledge_import WHERE knowledge_base_id = ?
//                """, id);
//        jdbc.update("DELETE FROM knowledge_base WHERE id = ?", id);
    }

    public List<Map<String, Object>> files(long id) {
//        get(id);
//        return jdbc.queryForList("""
//                SELECT file_hash, filename, created_at
//                FROM knowledge_import
//                WHERE knowledge_base_id = ?
//                ORDER BY created_at DESC
//                """, id);
        return null;
    }

    private static void validate(KnowledgeBaseDTO dto) {
        if (dto == null) {
            throw new BizException(400, "知识库参数不能为空");
        }
        required(dto.getName(), 100, "知识库名称");
        required(dto.getOwnerId(), 100, "负责人");
        if (description(dto.getDescription()).length() > 2000) {
            throw new BizException(400, "描述不能超过 2000 字符");
        }
    }

    private static String description(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }

    private static void required(String value, int max, String label) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw new BizException(400,
                    label + "不能为空且不能超过 " + max + " 字符");
        }
    }


}
