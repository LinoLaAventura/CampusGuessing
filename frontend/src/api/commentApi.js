import { apiClient, unwrapApiResponse } from './apiClient';

/**
 * 提交评论
 * POST /api/questions/{questionId}/comments
 * @param {string|number} questionId - 题目ID
 * @param {Object} data - { userId, content }
 */
export function createComment(questionId, { userId, content }) {
    // 确保 userId 是数字类型，符合后端 Long 类型要求
    const payload = {
        userId: Number(userId),
        content: content
    };

    return unwrapApiResponse(
        apiClient.post(`/questions/${questionId}/comments`, payload)
    );
}