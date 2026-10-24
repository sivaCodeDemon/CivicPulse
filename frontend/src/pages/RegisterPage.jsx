import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getApiError } from '../utils/helpers';
import { Zap, ArrowRight, Loader2, CheckCircle } from 'lucide-react';
import toast from 'react-hot-toast';

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    firstName: '', lastName: '', email: '', password: '', confirmPassword: '', phoneNumber: '',
  });
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (form.password !== form.confirmPassword) {
      toast.error('Passwords do not match');
      return;
    }
    setLoading(true);
    try {
      await register({
        firstName: form.firstName,
        lastName: form.lastName,
        email: form.email,
        password: form.password,
        phoneNumber: form.phoneNumber || undefined,
        role: 'ROLE_CITIZEN',
      });
      toast.success('Account created! Please sign in.');
      navigate('/login');
    } catch (err) {
      toast.error(getApiError(err));
    } finally {
      setLoading(false);
    }
  };

  const features = [
    'Submit civic complaints with GPS location',
    'AI-powered issue categorization',
    'Real-time status tracking & notifications',
    'Photo evidence uploads',
  ];

  return (
    <div className="min-h-screen flex items-center justify-center p-4 relative overflow-hidden"
         style={{ background: 'radial-gradient(ellipse at bottom right, #0a1525 0%, #0a0a0f 60%, #1a0a2e 100%)' }}>
      <div className="absolute top-0 right-1/4 w-80 h-80 bg-accent-purple/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-0 left-1/4 w-80 h-80 bg-primary-600/10 rounded-full blur-3xl pointer-events-none" />

      <div className="w-full max-w-4xl flex gap-8 items-start animate-slide-up">
        {/* Left branding panel */}
        <div className="hidden lg:flex flex-col justify-center flex-1 pr-8 pt-8">
          <div className="inline-flex items-center gap-2 w-fit px-3 py-1.5 rounded-full bg-primary-500/20 border border-primary-500/30 text-primary-300 text-xs font-semibold mb-6">
            <Zap size={12} /> AI-Powered Smart City
          </div>
          <h1 className="text-4xl font-display font-bold text-white leading-tight mb-4">
            Make your city<br />
            <span className="text-gradient">smarter together</span>
          </h1>
          <p className="text-white/50 text-base leading-relaxed mb-8">
            Join thousands of citizens actively shaping better urban infrastructure
            through intelligent reporting and AI-driven resolution.
          </p>
          <div className="space-y-3">
            {features.map((f) => (
              <div key={f} className="flex items-center gap-3">
                <CheckCircle size={16} className="text-accent-emerald flex-shrink-0" />
                <span className="text-white/70 text-sm">{f}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Register form */}
        <div className="flex-1 glass-card p-8">
          <div className="lg:hidden text-center mb-6">
            <div className="inline-flex items-center justify-center w-12 h-12 rounded-xl bg-gradient-to-br from-primary-500 to-accent-purple mb-3">
              <Zap size={22} className="text-white" />
            </div>
            <h2 className="text-xl font-display font-bold text-white">Create Account</h2>
          </div>
          <h2 className="hidden lg:block text-2xl font-display font-bold text-white mb-1">Create your account</h2>
          <p className="hidden lg:block text-white/40 text-sm mb-6">Register as a citizen to start reporting civic issues</p>

          <form onSubmit={handleSubmit} id="register-form" className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label htmlFor="reg-firstname" className="input-label">First Name</label>
                <input id="reg-firstname" name="firstName" required placeholder="Aarav"
                  className="input-field" value={form.firstName} onChange={handleChange} />
              </div>
              <div>
                <label htmlFor="reg-lastname" className="input-label">Last Name</label>
                <input id="reg-lastname" name="lastName" required placeholder="Sharma"
                  className="input-field" value={form.lastName} onChange={handleChange} />
              </div>
            </div>
            <div>
              <label htmlFor="reg-email" className="input-label">Email</label>
              <input id="reg-email" name="email" type="email" required placeholder="you@example.com"
                className="input-field" value={form.email} onChange={handleChange} />
            </div>
            <div>
              <label htmlFor="reg-phone" className="input-label">Phone (optional)</label>
              <input id="reg-phone" name="phoneNumber" type="tel" placeholder="+91 98765 43210"
                className="input-field" value={form.phoneNumber} onChange={handleChange} />
            </div>
            <div>
              <label htmlFor="reg-password" className="input-label">Password</label>
              <input id="reg-password" name="password" type="password" required minLength={8}
                placeholder="At least 8 characters"
                className="input-field" value={form.password} onChange={handleChange} />
            </div>
            <div>
              <label htmlFor="reg-confirm" className="input-label">Confirm Password</label>
              <input id="reg-confirm" name="confirmPassword" type="password" required
                placeholder="Re-enter your password"
                className="input-field" value={form.confirmPassword} onChange={handleChange} />
            </div>

            <button type="submit" id="register-submit-btn" disabled={loading}
              className="btn-primary w-full text-base py-3.5 mt-2">
              {loading ? (
                <><Loader2 size={18} className="animate-spin" /> Creating account...</>
              ) : (
                <>Create Account <ArrowRight size={18} /></>
              )}
            </button>
          </form>

          <p className="text-center text-white/40 text-sm mt-5">
            Already have an account?{' '}
            <Link to="/login" id="login-link" className="text-primary-400 hover:text-primary-300 font-semibold transition-colors">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
