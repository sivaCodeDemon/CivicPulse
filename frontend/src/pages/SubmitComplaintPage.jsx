import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { complaintsService } from '../services/complaints';
import { CATEGORIES } from '../utils/helpers';
import { getApiError } from '../utils/helpers';
import { MapPin, Upload, Loader2, ArrowRight, Locate, X, Image } from 'lucide-react';
import toast from 'react-hot-toast';

export default function SubmitComplaintPage() {
  const navigate = useNavigate();
  const { user } = useAuth();

  const [form, setForm] = useState({
    title: '',
    description: '',
    category: '',
    addressText: '',
    latitude: '',
    longitude: '',
    priority: '',
    severity: '',
  });
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [locating, setLocating] = useState(false);
  const [step, setStep] = useState(1); // 1: Details, 2: Location, 3: Photos

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleGeolocate = () => {
    if (!navigator.geolocation) {
      toast.error('Geolocation not supported by your browser');
      return;
    }
    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setForm((f) => ({
          ...f,
          latitude: pos.coords.latitude.toString(),
          longitude: pos.coords.longitude.toString(),
        }));
        setLocating(false);
        toast.success('Location captured!');
      },
      () => {
        setLocating(false);
        toast.error('Unable to get location. Please enter manually.');
      },
      { timeout: 10000 }
    );
  };

  const handleFileChange = (e) => {
    const selected = Array.from(e.target.files).slice(0, 5);
    setFiles((prev) => [...prev, ...selected].slice(0, 5));
  };

  const removeFile = (idx) => setFiles((prev) => prev.filter((_, i) => i !== idx));

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.category) { toast.error('Please select a category'); return; }
    if (!form.latitude || !form.longitude) { toast.error('Location is required'); return; }

    setLoading(true);
    try {
      const payload = {
        title: form.title,
        description: form.description,
        category: form.category,
        addressText: form.addressText || null,
        latitude: parseFloat(form.latitude),
        longitude: parseFloat(form.longitude),
        priority: form.priority || null,
        severity: form.severity || null,
      };

      const res = await complaintsService.create(payload);
      const complaintId = res.data?.data?.id;

      // Upload images
      if (files.length > 0 && complaintId) {
        for (const file of files) {
          await complaintsService.uploadImage(complaintId, file).catch(() => {});
        }
      }

      toast.success('Complaint submitted successfully! Our AI is analyzing your report.');
      navigate(`/complaints/${complaintId}`);
    } catch (err) {
      toast.error(getApiError(err));
    } finally {
      setLoading(false);
    }
  };

  const steps = [
    { n: 1, label: 'Details' },
    { n: 2, label: 'Location' },
    { n: 3, label: 'Photos' },
  ];

  return (
    <div className="max-w-2xl mx-auto space-y-6 animate-fade-in">
      {/* Header */}
      <div>
        <h1 className="section-title">Submit Civic Issue</h1>
        <p className="text-white/40 text-sm mt-1">Our AI will automatically analyze and categorize your report</p>
      </div>

      {/* Step progress */}
      <div className="flex items-center gap-0">
        {steps.map(({ n, label }, i) => (
          <div key={n} className="flex items-center flex-1">
            <button
              type="button"
              onClick={() => setStep(n)}
              className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium transition-all
                ${step === n
                  ? 'bg-primary-600/20 text-primary-300 border border-primary-500/30'
                  : step > n
                  ? 'text-emerald-400 bg-emerald-500/10'
                  : 'text-white/30'
                }`}
            >
              <span className={`w-5 h-5 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0
                ${step === n ? 'bg-primary-500 text-white' : step > n ? 'bg-emerald-500 text-white' : 'bg-white/10 text-white/30'}`}>
                {step > n ? '✓' : n}
              </span>
              {label}
            </button>
            {i < steps.length - 1 && <div className="h-px flex-1 bg-white/10 mx-1" />}
          </div>
        ))}
      </div>

      <form onSubmit={handleSubmit} id="submit-complaint-form">
        {/* Step 1: Details */}
        {step === 1 && (
          <div className="glass-card p-6 space-y-5 animate-slide-up">
            <div>
              <label className="input-label">Issue Title *</label>
              <input name="title" required placeholder="e.g., Large pothole near school gate"
                className="input-field" value={form.title} onChange={handleChange} maxLength={255} />
              <p className="text-xs text-white/25 mt-1">{form.title.length}/255</p>
            </div>
            <div>
              <label className="input-label">Category *</label>
              <div className="grid grid-cols-3 gap-2 mt-1">
                {CATEGORIES.map(({ value, label, icon }) => (
                  <button
                    key={value}
                    type="button"
                    id={`cat-${value}`}
                    onClick={() => setForm({ ...form, category: value })}
                    className={`flex flex-col items-center gap-1.5 p-3 rounded-xl border text-sm font-medium transition-all
                      ${form.category === value
                        ? 'bg-primary-600/20 border-primary-500/50 text-primary-300'
                        : 'bg-white/5 border-white/10 text-white/50 hover:bg-white/10 hover:text-white/80'
                      }`}
                  >
                    <span className="text-xl">{icon}</span>
                    <span className="text-xs text-center leading-tight">{label}</span>
                  </button>
                ))}
              </div>
            </div>
            <div>
              <label className="input-label">Description *</label>
              <textarea name="description" required rows={4}
                placeholder="Describe the issue in detail — severity, location specifics, safety concerns..."
                className="input-field resize-none"
                value={form.description} onChange={handleChange} maxLength={2000} />
              <p className="text-xs text-white/25 mt-1">{form.description.length}/2000</p>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="input-label">Priority (optional)</label>
                <select name="priority" className="input-field" value={form.priority} onChange={handleChange}>
                  <option value="">Auto (AI will suggest)</option>
                  <option value="LOW">Low</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="HIGH">High</option>
                  <option value="CRITICAL">Critical</option>
                </select>
              </div>
              <div>
                <label className="input-label">Severity (optional)</label>
                <select name="severity" className="input-field" value={form.severity} onChange={handleChange}>
                  <option value="">Auto (AI will suggest)</option>
                  <option value="MINOR">Minor</option>
                  <option value="MODERATE">Moderate</option>
                  <option value="SEVERE">Severe</option>
                  <option value="EMERGENCY">Emergency</option>
                </select>
              </div>
            </div>
            <button type="button" onClick={() => setStep(2)} className="btn-primary w-full">
              Next: Location <ArrowRight size={16} />
            </button>
          </div>
        )}

        {/* Step 2: Location */}
        {step === 2 && (
          <div className="glass-card p-6 space-y-5 animate-slide-up">
            <div className="flex items-center gap-3 p-4 rounded-xl bg-primary-500/10 border border-primary-500/20">
              <MapPin size={20} className="text-primary-400 flex-shrink-0" />
              <p className="text-sm text-white/70">
                Accurate location helps municipal officers respond faster. Use auto-detect or enter coordinates manually.
              </p>
            </div>
            <button
              type="button"
              id="geolocate-btn"
              onClick={handleGeolocate}
              disabled={locating}
              className="btn-secondary w-full gap-2"
            >
              {locating ? <Loader2 size={16} className="animate-spin" /> : <Locate size={16} />}
              {locating ? 'Detecting location...' : 'Auto-detect my location'}
            </button>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="input-label">Latitude *</label>
                <input name="latitude" type="number" step="any" required
                  placeholder="12.9716"
                  className="input-field" value={form.latitude} onChange={handleChange} />
              </div>
              <div>
                <label className="input-label">Longitude *</label>
                <input name="longitude" type="number" step="any" required
                  placeholder="77.5946"
                  className="input-field" value={form.longitude} onChange={handleChange} />
              </div>
            </div>
            <div>
              <label className="input-label">Address / Landmark</label>
              <input name="addressText" placeholder="Near Brigade Road, Bengaluru"
                className="input-field" value={form.addressText} onChange={handleChange} maxLength={500} />
            </div>
            <div className="flex gap-3">
              <button type="button" onClick={() => setStep(1)} className="btn-secondary flex-1">Back</button>
              <button type="button" onClick={() => setStep(3)} className="btn-primary flex-1">
                Next: Photos <ArrowRight size={16} />
              </button>
            </div>
          </div>
        )}

        {/* Step 3: Photos */}
        {step === 3 && (
          <div className="glass-card p-6 space-y-5 animate-slide-up">
            <div>
              <label className="input-label">Upload Photos (optional, max 5)</label>
              <label htmlFor="photo-upload"
                className="flex flex-col items-center justify-center gap-3 p-8 rounded-xl border-2 border-dashed border-white/15 hover:border-primary-500/40 hover:bg-primary-500/5 transition-all cursor-pointer">
                <div className="w-12 h-12 rounded-xl bg-white/5 flex items-center justify-center">
                  <Upload size={24} className="text-white/40" />
                </div>
                <div className="text-center">
                  <p className="text-white/60 text-sm font-medium">Click to upload photos</p>
                  <p className="text-white/30 text-xs mt-1">JPEG, PNG, WebP up to 10MB each</p>
                </div>
                <input id="photo-upload" type="file" multiple accept="image/*"
                  className="hidden" onChange={handleFileChange} />
              </label>
            </div>

            {files.length > 0 && (
              <div className="grid grid-cols-2 gap-3">
                {files.map((f, i) => (
                  <div key={i} className="relative flex items-center gap-2 p-3 rounded-xl bg-white/5 border border-white/10">
                    <Image size={18} className="text-primary-400 flex-shrink-0" />
                    <p className="text-xs text-white/60 truncate flex-1">{f.name}</p>
                    <button type="button" onClick={() => removeFile(i)}
                      className="text-white/30 hover:text-rose-400 transition-colors">
                      <X size={14} />
                    </button>
                  </div>
                ))}
              </div>
            )}

            <div className="flex gap-3">
              <button type="button" onClick={() => setStep(2)} className="btn-secondary flex-1">Back</button>
              <button type="submit" id="submit-final-btn" disabled={loading} className="btn-primary flex-1">
                {loading ? (
                  <><Loader2 size={16} className="animate-spin" /> Submitting...</>
                ) : (
                  <>Submit Report <ArrowRight size={16} /></>
                )}
              </button>
            </div>
          </div>
        )}
      </form>
    </div>
  );
}
