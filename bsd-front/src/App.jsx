import { useState } from 'react'
import { Upload, FileSpreadsheet, Truck, CheckCircle, AlertCircle, Loader2, Beer, List, LogIn, Lock, User } from 'lucide-react'
import PedidosTable from './PedidosTable'
import './index.css'


function App() {
  
  const [usuario, setUsuario] = useState(null)
  const [usernameInput, setUsernameInput] = useState('')
  const [passwordInput, setPasswordInput] = useState('')
  const [loginError, setLoginError] = useState('')


  const [view, setView] = useState('upload') 
  const [fileData, setFileData] = useState(null)
  const [fileRuta, setFileRuta] = useState(null)
  const [transportesPiso, setTransportesPiso] = useState('')
  const [esNuevoDia, setEsNuevoDia] = useState(true) 
  const [loading, setLoading] = useState(false)
  const [status, setStatus] = useState({ type: '', message: '' })


  const handleLogin = (e) => {
    e.preventDefault()
    if (usernameInput.toLowerCase() === 'admin' && passwordInput === 'Bavaria2026') {
      setUsuario(usernameInput)
      setLoginError('')
    } else {
      setLoginError('Credenciales incorrectas. Intente nuevamente.')
    }
  }

  
  const handleUpload = async (e) => {
    e.preventDefault()
    if (!fileData || !fileRuta) {
      setStatus({ type: 'error', message: '¡Atención! Faltan archivos por seleccionar.' })
      return
    }

    setLoading(true)
    setStatus({ type: '', message: '' })


    if (esNuevoDia) {
      try {
        await fetch('http://localhost:8080/api/admin/limpiar-pedidos', {
          method: 'DELETE'
        });
        console.log("🧹 Base de datos de pedidos limpiada correctamente.");
      } catch (err) {
        console.error("No se pudo limpiar la base, procediendo con la carga...", err);
      }
    }

    const formData = new FormData()
    formData.append('fileData', fileData)
    formData.append('fileRuta', fileRuta)
    formData.append('transportesPiso', transportesPiso)
    formData.append('limpiarDb', esNuevoDia) 

    try {
      const auth = btoa('admin:bavaria2026'); 
      const response = await fetch('http://localhost:8080/api/carga/pedidos', {
        method: 'POST',
        headers: { 'Authorization': `Basic ${auth}` },
        body: formData
      })

      if (response.ok) {
        const text = await response.text()
        setStatus({ type: 'success', message: '¡Proceso Exitoso! ' + text })
        setTimeout(() => {
            setStatus({ type: '', message: '' })
            setView('table')
        }, 2000)
      } else {
        const errorText = await response.text()
        setStatus({ type: 'error', message: `Error del servidor: ${errorText}` })
      }
    } catch (error) {
      console.error(error)
      setStatus({ type: 'error', message: 'Error' })
    } finally {
      setLoading(false)
    }
  }


  if (!usuario) {
    return (
      <div className="login-container">
        <div className="login-card fade-in">
          <div className="login-header">
            <div className="logo-circle"><Beer size={40} color="white" /></div>
            <h1>Bavaria Smart Dispatch</h1>
            <p>logueate, asi sabemos quien eres </p>
          </div>
          <form onSubmit={handleLogin}>
            <div className="input-group">
                <div className="input-icon"><User size={18}/></div>
                <input type="text" placeholder="Usuario" className="login-input" value={usernameInput} onChange={e => setUsernameInput(e.target.value)} autoFocus />
            </div>
            <div className="input-group">
                <div className="input-icon"><Lock size={18}/></div>
                <input type="password" placeholder="Contraseña" className="login-input" value={passwordInput} onChange={e => setPasswordInput(e.target.value)} />
            </div>
            {loginError && <div className="login-error"><AlertCircle size={16}/> {loginError}</div>}
            <button type="submit" className="btn-login">INGRESAR <LogIn size={18} /></button>
          </form>
          <div className="login-footer">© 2026 Bavaria S.A.</div>
        </div>
      </div>
    )
  }

  // --- VISTA PRINCIPAL ---
  return (
    <div className="container">
      <div className="header">
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '10px' }}>
          <div style={{ background: '#D92D20', padding: '12px', borderRadius: '50%' }}>
            <Beer size={40} color="white" />
          </div>
        </div>
        <h1>Bavaria Smart Dispatch</h1>
        <p>Centro de Control de Distribución</p>
        <button className="btn-logout" onClick={() => setUsuario(null)}>Cerrar Sesión</button>
      </div>

      <div className="nav-tabs">
        <button className={`nav-btn ${view === 'upload' ? 'active' : ''}`} onClick={() => setView('upload')}>
          <Upload size={16} style={{marginRight:5, display:'inline'}}/> Cargar Archivos
        </button>
        <button className={`nav-btn ${view === 'table' ? 'active' : ''}`} onClick={() => setView('table')}>
          <List size={16} style={{marginRight:5, display:'inline'}}/> Gestión de Envíos
        </button>
      </div>

      {view === 'upload' ? (
        <div className="card fade-in">
          <form onSubmit={handleUpload}>
            <div className="new-day-box">
                <div style={{display:'flex', alignItems:'center', gap:'15px'}}>
                    <input type="checkbox" id="checkLimpiar" checked={esNuevoDia} onChange={(e) => setEsNuevoDia(e.target.checked)} className="big-checkbox" />
                    <div>
                        <label htmlFor="checkLimpiar" className="label-strong">🧹 ¿inicia un nuevo dia?, Limpia tu base</label>
                        <small>Marca: para borrar la base | Desmarcado: Agrega pedidos.</small>
                    </div>
                </div>
            </div>
            <div className="grid-2-col">
                <div className="input-group">
                <label>1. Archivo Data</label>
                <div className="file-drop-zone">
                    <FileSpreadsheet size={32} color={fileData ? "#166534" : "#D92D20"} />
                    <p>{fileData ? fileData.name : "Seleccionar DATA"}</p>
                    <input type="file" onChange={(e) => setFileData(e.target.files[0])} />
                </div>
                </div>
                <div className="input-group">
                <label>2. Archivo Ruta</label>
                <div className="file-drop-zone">
                    <Truck size={32} color={fileRuta ? "#166534" : "#D92D20"} />
                    <p>{fileRuta ? fileRuta.name : "Seleccionar RUTA"}</p>
                    <input type="file" onChange={(e) => setFileRuta(e.target.files[0])} />
                </div>
                </div>
            </div>
            <div className="input-group">
              <label>3. Transportes en PISO</label>
              <textarea rows="2" placeholder="8008465122, 8008434524, 8008...." value={transportesPiso} onChange={(e) => setTransportesPiso(e.target.value)} />
            </div>
            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? <Loader2 className="animate-spin" /> : "CARGAR ARCHIVOS"}
            </button>
          </form>
          {status.message && <div className={`status-box ${status.type}`}>{status.message}</div>}
        </div>
      ) : (
        <PedidosTable />
        ) }
    </div>
  )
}
export default App