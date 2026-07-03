import { get, withQueryParams } from './apiClient'

const AUDIT_PATH = '/api/audit'

export function listAuditRecords(params = {}) {
  return get(withQueryParams(AUDIT_PATH, params))
}

export function getAuditRecord(auditId) {
  return get(`${AUDIT_PATH}/${encodeURIComponent(auditId)}`)
}
