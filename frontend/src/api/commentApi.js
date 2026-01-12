
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

    return (async () => {
        const response = await apiClient.post(`/questions/${questionId}/comments`, payload);
        return unwrapApiResponse(response.data);
    })();
}

/**
 * 获取评论列表
 * GET /api/questions/{questionId}/comments
 */
export async function getCommentList(questionId) {
    const response = await apiClient.get(`/questions/${questionId}/comments`);
    console.log("获取评论列表响应:", response);
    return unwrapApiResponse(response.data);

}

/**
 * 删除评论
 * DELETE /api/comments/{commentId}
 * Body: { "userId": 5 }
 */
export function deleteComment(commentId, userId) {
    console.log(`删除评论请求 - commentId: ${commentId}, userId: ${userId}`);
    return (async () => {
        const response = await apiClient.delete(`/comments/${commentId}`, {
            data: { userId: Number(userId) } // axios delete 的 body 需要放在 data 字段里
        });
        return unwrapApiResponse(response.data);
    })();
}

/**
 * 点赞评论
 * POST /api/comments/{commentId}/likes
 */
export async function likeComment(commentId, userId) {
    // 根据你的描述，路径应该是 /comments/{id}/likes
    // 但根据项目惯例，前面通常加 /api，这里保持与 deleteComment 一致的风格
    const response = await apiClient.post(`/comments/${commentId}/likes`, {
        userId: Number(userId)
    });
    return unwrapApiResponse(response.data);
}

/**
 * 取消点赞评论
 * DELETE /api/comments/{commentId}/likes
 */
export async function unlikeComment(commentId, userId) {
    const response = await apiClient.delete(`/comments/${commentId}/likes`, {
        data: { userId: Number(userId) }
    });
    return unwrapApiResponse(response.data);
}