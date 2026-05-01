import React, { useState } from 'react';

export default function ActionModal({
  type,
  issue,
  departments,
  onAction,
  onClose,
  loading,
  message,
  adminRole,
}) {
  const [formData, setFormData] = useState({
    departmentId: '',
    toDistrictId: '',
    remarks: '',
    newStatus: '',
    finalRemarks: '',
    proofImageUrl: '',
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    switch (type) {
      case 'forward-to-district':
        onAction({
          type: 'forward-to-district',
          toDistrictId: formData.toDistrictId,
          remarks: formData.remarks,
        });
        break;
      case 'assign':
        onAction({
          type: 'assign',
          departmentId: formData.departmentId,
          remarks: formData.remarks,
        });
        break;
      case 'close':
        onAction({
          type: 'close',
          finalRemarks: formData.finalRemarks,
          proofImageUrl: formData.proofImageUrl,
        });
        break;
      case 'submit-proof':
        onAction({
          type: 'submit-proof',
          remarks: formData.remarks,
          proofImageUrl: formData.proofImageUrl,
        });
        break;
      case 'status':
        onAction({
          type: 'status',
          newStatus: formData.newStatus,
          remarks: formData.remarks,
        });
        break;
      case 'forward':
        onAction({
          type: 'forward',
          departmentId: formData.departmentId,
          remarks: formData.remarks,
        });
        break;
      case 'remarks':
        onAction({
          type: 'remarks',
          remarks: formData.remarks,
        });
        break;
    }
  };

  const getTitle = () => {
    switch (type) {
      case 'forward-to-district':
        return 'Forward to District';
      case 'assign':
        return adminRole === 'DISTRICT' ? 'Assign to Department' : 'Assign Issue';
      case 'close':
        return adminRole === 'DISTRICT' ? 'Close After Review' : 'Close Issue with Proof';
      case 'submit-proof':
        return 'Submit Proof to District Admin';
      case 'status':
        return 'Update Status';
      case 'forward':
        return 'Forward Issue';
      case 'remarks':
        return 'Add Remarks';
      default:
        return 'Action';
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>{getTitle()}</h2>
          <button className="modal-close" onClick={onClose} aria-label="Close">
            ✕
          </button>
        </div>

        {issue && (
          <div className="modal-issue-header">
            <strong>#{issue.id}</strong>
            <span>{issue.title}</span>
          </div>
        )}

        {message.text && (
          <div className={`alert alert-${message.type}`}>
            {message.text}
          </div>
        )}

        <form onSubmit={handleSubmit} className="modal-form">
          {/* STATE_ADMIN: Forward to District */}
          {type === 'forward-to-district' && (
            <>
              <div className="form-group">
                <label htmlFor="toDistrictId">Forward to District</label>
                <select
                  id="toDistrictId"
                  name="toDistrictId"
                  value={formData.toDistrictId}
                  onChange={handleChange}
                  required
                >
                  <option value="">Select a district</option>
                  {departments.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label htmlFor="remarks">Remarks</label>
                <textarea
                  id="remarks"
                  name="remarks"
                  value={formData.remarks}
                  onChange={handleChange}
                  placeholder="Enter forwarding remarks..."
                  rows="4"
                  required
                />
              </div>
            </>
          )}

          {/* DISTRICT_ADMIN: Assign to Department */}
          {type === 'assign' && adminRole === 'DISTRICT' && (
            <>
              <div className="form-group">
                <label htmlFor="departmentId">Assign to Department</label>
                <select
                  id="departmentId"
                  name="departmentId"
                  value={formData.departmentId}
                  onChange={handleChange}
                  required
                >
                  <option value="">Select a department</option>
                  {departments.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label htmlFor="remarks">Assignment Remarks</label>
                <textarea
                  id="remarks"
                  name="remarks"
                  value={formData.remarks}
                  onChange={handleChange}
                  placeholder="Explain why this issue is being assigned to this department..."
                  rows="4"
                  required
                />
              </div>
            </>
          )}

          {/* DISTRICT_ADMIN: Close After Review */}
          {type === 'close' && adminRole === 'DISTRICT' && (
            <>
              <div className="form-group">
                <label htmlFor="finalRemarks">Review Remarks</label>
                <textarea
                  id="finalRemarks"
                  name="finalRemarks"
                  value={formData.finalRemarks}
                  onChange={handleChange}
                  placeholder="Final review remarks before closing..."
                  rows="4"
                  required
                />
              </div>
              <div className="form-group">
                <label htmlFor="proofImageUrl">
                  Proof Image URL (from department)
                </label>
                <input
                  id="proofImageUrl"
                  type="url"
                  name="proofImageUrl"
                  value={formData.proofImageUrl}
                  onChange={handleChange}
                  placeholder="https://example.com/proof.jpg"
                  required
                />
              </div>
            </>
          )}

          {/* DEPARTMENT_ADMIN: Submit Proof */}
          {type === 'submit-proof' && (
            <>
              <div className="form-group">
                <label htmlFor="remarks">Work Completion Remarks</label>
                <textarea
                  id="remarks"
                  name="remarks"
                  value={formData.remarks}
                  onChange={handleChange}
                  placeholder="Describe the work completed and actions taken..."
                  rows="4"
                  required
                />
              </div>
              <div className="form-group">
                <label htmlFor="proofImageUrl">
                  Proof Image URL (completion proof)
                </label>
                <input
                  id="proofImageUrl"
                  type="url"
                  name="proofImageUrl"
                  value={formData.proofImageUrl}
                  onChange={handleChange}
                  placeholder="https://example.com/proof.jpg"
                  required
                />
              </div>
            </>
          )}

          {/* DEPARTMENT_ADMIN: Update Status */}
          {type === 'status' && adminRole === 'AUTHORITY' && (
            <>
              <div className="form-group">
                <label htmlFor="newStatus">New Status</label>
                <select
                  id="newStatus"
                  name="newStatus"
                  value={formData.newStatus}
                  onChange={handleChange}
                  required
                >
                  <option value="">Select a status</option>
                  <option value="in_progress">In Progress</option>
                  <option value="resolved">Resolved</option>
                </select>
              </div>
              <div className="form-group">
                <label htmlFor="remarks">Status Update Remarks</label>
                <textarea
                  id="remarks"
                  name="remarks"
                  value={formData.remarks}
                  onChange={handleChange}
                  placeholder="Add remarks about this status update..."
                  rows="3"
                  required
                />
              </div>
            </>
          )}

          {/* Generic forms for backward compatibility */}
          {(type === 'assign' || type === 'forward') && !adminRole && (
            <>
              <div className="form-group">
                <label htmlFor="departmentId">
                  {type === 'assign' ? 'Assign to Department' : 'Forward to Department'}
                </label>
                <select
                  id="departmentId"
                  name="departmentId"
                  value={formData.departmentId}
                  onChange={handleChange}
                  required
                >
                  <option value="">Select a department</option>
                  {departments
                    .filter((d) => d.id !== '')
                    .map((d) => (
                      <option key={d.id} value={d.id}>
                        {d.name}
                      </option>
                    ))}
                </select>
              </div>
              <div className="form-group">
                <label htmlFor="remarks">Remarks</label>
                <textarea
                  id="remarks"
                  name="remarks"
                  value={formData.remarks}
                  onChange={handleChange}
                  placeholder="Enter your remarks..."
                  rows="4"
                  required
                />
              </div>
            </>
          )}

          {(type === 'remarks' || (type === 'status' && !adminRole)) && (
            <div className="form-group">
              <label htmlFor="remarks">Remarks</label>
              <textarea
                id="remarks"
                name="remarks"
                value={formData.remarks}
                onChange={handleChange}
                placeholder="Enter your remarks..."
                rows="4"
                required
              />
            </div>
          )}

          {type === 'status' && !adminRole && (
            <div className="form-group">
              <label htmlFor="newStatus">New Status</label>
              <select
                id="newStatus"
                name="newStatus"
                value={formData.newStatus}
                onChange={handleChange}
                required
              >
                <option value="">Select a status</option>
                <option value="open">Open</option>
                <option value="assigned">Assigned</option>
                <option value="in_progress">In Progress</option>
                <option value="forwarded">Forwarded</option>
                <option value="resolved">Resolved</option>
                <option value="closed">Closed</option>
              </select>
            </div>
          )}

          {type === 'close' && !adminRole && (
            <>
              <div className="form-group">
                <label htmlFor="finalRemarks">Final Remarks</label>
                <textarea
                  id="finalRemarks"
                  name="finalRemarks"
                  value={formData.finalRemarks}
                  onChange={handleChange}
                  placeholder="Enter final remarks..."
                  rows="4"
                  required
                />
              </div>
              <div className="form-group">
                <label htmlFor="proofImageUrl">
                  Proof Image URL
                </label>
                <input
                  id="proofImageUrl"
                  type="url"
                  name="proofImageUrl"
                  value={formData.proofImageUrl}
                  onChange={handleChange}
                  placeholder="https://example.com/proof.jpg"
                  required
                />
              </div>
            </>
          )}

          <div className="modal-footer">
            <button
              type="button"
              className="btn-secondary"
              onClick={onClose}
              disabled={loading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn-primary"
              disabled={loading}
            >
              {loading ? 'Processing...' : 'Confirm'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
