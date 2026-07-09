import axios from '@/utils/axios'

export const createAlignmentTurn = (data) => {
  return axios.post('/aico/alignment/turns', data)
}

export const getPersonalAlignment = (userId) => {
  return axios.get(`/aico/alignment/users/${userId}/state`)
}

export const getRelationshipAlignment = (userId, counterpartId) => {
  return axios.get('/aico/alignment/relationships', {
    params: { userId, counterpartId }
  })
}

export const submitAlignmentFeedback = (data) => {
  return axios.post('/aico/alignment/feedback', data)
}
