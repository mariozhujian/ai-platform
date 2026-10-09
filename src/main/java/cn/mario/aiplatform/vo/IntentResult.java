package cn.mario.aiplatform.vo;

import cn.mario.aiplatform.enums.IntentTypeEnum;

/**
 * 意图结果
 * @param intent 售后类型
 * @param confidence
 * @param reason 原因
 * @param needHuman 是否需要人工客服
 */
public record IntentResult(IntentTypeEnum intent,
                           double confidence,
                           String reason,
                           boolean needHuman) {
}
