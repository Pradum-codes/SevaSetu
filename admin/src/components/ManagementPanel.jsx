import React, { useEffect, useState } from 'react';
import { adminApi } from '../api';

const initialHeadForm = {
  email: '',
  name: '',
  phone: '',
  password: '',
  districtId: '',
  departmentId: '',
};

export default function ManagementPanel({ adminRole, districts = [], departments = [] }) {
  const [items, setItems] = useState([]);
  const [heads, setHeads] = useState([]);
  const [form, setForm] = useState({ name: '', category: 'URBAN', pincode: '' });
  const [headForm, setHeadForm] = useState(initialHeadForm);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });

  const isState = adminRole === 'STATE';
  const isDistrict = adminRole === 'DISTRICT';

  const loadData = async () => {
    if (!isState && !isDistrict) return;

    setLoading(true);
    setMessage({ type: '', text: '' });

    try {
      if (isState) {
        const [districtResponse, headResponse] = await Promise.all([
          adminApi.stateAdminListDistricts(),
          adminApi.stateAdminListDistrictHeads(),
        ]);
        setItems(districtResponse.districts || []);
        setHeads(headResponse.heads || []);
      } else {
        const [departmentResponse, headResponse] = await Promise.all([
          adminApi.districtAdminListDepartments(),
          adminApi.districtAdminListDepartmentHeads(),
        ]);
        setItems(departmentResponse.departments || []);
        setHeads(headResponse.heads || []);
      }
    } catch (error) {
      setMessage({ type: 'error', text: error.message });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [adminRole]);

  const handleFormChange = (event) => {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleHeadFormChange = (event) => {
    const { name, value } = event.target;
    setHeadForm((prev) => ({ ...prev, [name]: value }));
  };

  const createItem = async (event) => {
    event.preventDefault();
    setLoading(true);
    setMessage({ type: '', text: '' });

    try {
      if (isState) {
        await adminApi.stateAdminCreateDistrict(form);
        setForm({ name: '', category: 'URBAN', pincode: '' });
        setMessage({ type: 'success', text: 'District created' });
      } else {
        await adminApi.districtAdminCreateDepartment({ name: form.name });
        setForm({ name: '', category: 'URBAN', pincode: '' });
        setMessage({ type: 'success', text: 'Department created' });
      }
      await loadData();
    } catch (error) {
      setMessage({ type: 'error', text: error.message });
    } finally {
      setLoading(false);
    }
  };

  const createHead = async (event) => {
    event.preventDefault();
    setLoading(true);
    setMessage({ type: '', text: '' });

    try {
      if (isState) {
        await adminApi.stateAdminCreateDistrictHead(headForm);
        setMessage({ type: 'success', text: 'District head created' });
      } else {
        await adminApi.districtAdminCreateDepartmentHead(headForm);
        setMessage({ type: 'success', text: 'Department head created' });
      }
      setHeadForm(initialHeadForm);
      await loadData();
    } catch (error) {
      setMessage({ type: 'error', text: error.message });
    } finally {
      setLoading(false);
    }
  };

  if (!isState && !isDistrict) {
    return null;
  }

  return (
    <section className="management-grid" aria-label="Management panel">
      {message.text && (
        <div className={`alert alert-${message.type}`}>
          {message.text}
        </div>
      )}

      <section className="management-card">
        <div className="section-heading compact">
          <h2>{isState ? 'Districts' : 'Departments'}</h2>
        </div>

        <form className="management-form" onSubmit={createItem}>
          <label>
            Name
            <input
              name="name"
              value={form.name}
              onChange={handleFormChange}
              required
              placeholder={isState ? 'New district name' : 'New department name'}
            />
          </label>

          {isState && (
            <>
              <label>
                Category
                <select name="category" value={form.category} onChange={handleFormChange}>
                  <option value="URBAN">Urban</option>
                  <option value="RURAL">Rural</option>
                </select>
              </label>
              <label>
                Pincode
                <input
                  name="pincode"
                  value={form.pincode}
                  onChange={handleFormChange}
                  placeholder="Optional"
                />
              </label>
            </>
          )}

          <button className="btn-primary" type="submit" disabled={loading}>
            Create
          </button>
        </form>

        <div className="management-list">
          {items.map((item) => (
            <article key={item.id} className="management-row">
              <strong>{item.name}</strong>
              {item.category && <span>{item.category}</span>}
            </article>
          ))}
        </div>
      </section>

      <section className="management-card">
        <div className="section-heading compact">
          <h2>{isState ? 'District Heads' : 'Department Heads'}</h2>
        </div>

        <form className="management-form" onSubmit={createHead}>
          <label>
            Name
            <input name="name" value={headForm.name} onChange={handleHeadFormChange} required />
          </label>
          <label>
            Email
            <input
              type="email"
              name="email"
              value={headForm.email}
              onChange={handleHeadFormChange}
              required
            />
          </label>
          <label>
            Phone
            <input name="phone" value={headForm.phone} onChange={handleHeadFormChange} />
          </label>
          <label>
            Password
            <input
              type="password"
              name="password"
              value={headForm.password}
              onChange={handleHeadFormChange}
              required
            />
          </label>

          {isState ? (
            <label>
              District
              <select
                name="districtId"
                value={headForm.districtId}
                onChange={handleHeadFormChange}
                required
              >
                <option value="">Select district</option>
                {items.map((district) => (
                  <option key={district.id} value={district.id}>
                    {district.name}
                  </option>
                ))}
              </select>
            </label>
          ) : (
            <label>
              Department
              <select
                name="departmentId"
                value={headForm.departmentId}
                onChange={handleHeadFormChange}
                required
              >
                <option value="">Select department</option>
                {(departments.length > 0 ? departments : items).map((department) => (
                  <option key={department.id} value={department.id}>
                    {department.name}
                  </option>
                ))}
              </select>
            </label>
          )}

          <button className="btn-primary" type="submit" disabled={loading}>
            Create Head
          </button>
        </form>

        <div className="management-list">
          {heads.map((head) => (
            <article key={head.id} className="management-row">
              <strong>{head.name || head.email}</strong>
              <span>{head.authorityProfile?.jurisdiction?.name}</span>
              <small>{head.authorityProfile?.department?.name}</small>
            </article>
          ))}
        </div>
      </section>
    </section>
  );
}
