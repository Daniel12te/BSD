
import { useState, useEffect } from 'react';
import { Search, RefreshCw, Truck, MapPin, User, FileDown, MessageCircle, Phone } from 'lucide-react';

export default function PedidosTable() {
    const [pedidos, setPedidos] = useState([]);
    const [loading, setLoading] = useState(false);
    
    
    const [seleccionados, setSeleccionados] = useState(new Set());
    const [filtros, setFiltros] = useState({
        busquedaGlobal: '',
        estadoLogistico: '',
        estadoNotificacion: 'TODOS' // PENDIENTE, CONFIRMADO, RECHAZADO
    });

    const fetchPedidos = async () => {
        setLoading(true);
        try {
            const auth = btoa('admin:bavaria2026');

            const response = await fetch(`http://localhost:8080/api/carga/pedidos?t=${new Date().getTime()}`, {
                headers: { 
                    'Authorization': `Basic ${auth}`,
                    'Cache-Control': 'no-cache' 
                }
            });
            if (response.ok) {
                const data = await response.json();
                setPedidos(data);
                setSeleccionados(new Set()); 
            }
        } catch (error) {
            console.error("Error cargando pedidos:", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchPedidos();
    }, []);

    const pedidosFiltrados = pedidos.filter(p => {
        const texto = filtros.busquedaGlobal.toLowerCase();
        
        const numeroEntrega = p.numeroEntrega?.toLowerCase() || '';
        const nombreCliente = p.cliente?.nombre?.toLowerCase() || '';
        const nombreConductor = p.nombreConductor?.toLowerCase() || '';
        const placa = p.placaVehiculo?.toLowerCase() || '';

        const coincideTexto = 
            numeroEntrega.includes(texto) ||
            nombreCliente.includes(texto) ||
            nombreConductor.includes(texto) ||
            placa.includes(texto);

        const coincideLogistico = filtros.estadoLogistico ? p.estadoLogistico === filtros.estadoLogistico : true;
        
        let coincideNotificacion = true;
        if (filtros.estadoNotificacion !== 'TODOS') {
            const estadoReal = p.estadoNotificacion || 'PENDIENTE'; // Si es null, es pendiente
            coincideNotificacion = estadoReal === filtros.estadoNotificacion;
        }

        return coincideTexto && coincideLogistico && coincideNotificacion;
    });

    const handleSelectAll = (e) => {
        if (e.target.checked) {
            const todosIds = new Set(pedidosFiltrados.map(p => p.id));
            setSeleccionados(todosIds);
        } else {
            setSeleccionados(new Set());
        }
    };

    const handleSelectOne = (id) => {
        const nuevos = new Set(seleccionados);
        if (nuevos.has(id)) nuevos.delete(id);
        else nuevos.add(id);
        setSeleccionados(nuevos);
    };

    const enviarNotificaciones = async () => {
        if (seleccionados.size === 0) return;

        const listaIds = Array.from(seleccionados);
        
        const confirmar = window.confirm(`¿Seguro que deseas enviar mensajes a ${listaIds.length} clientes?`);
        if (!confirmar) return;

        setLoading(true); 

        try {
            const auth = btoa('admin:bavaria2026');
            
            
            const response = await fetch('http://localhost:8080/api/notificaciones/enviar', {
                method: 'POST',
                headers: { 
                    'Authorization': `Basic ${auth}`,
                    'Content-Type': 'application/json' 
                },
                body: JSON.stringify(listaIds) 
            });

            if (response.ok) {
                const mensaje = await response.text();
                alert("✅ ÉXITO: " + mensaje);
                
                setTimeout(() => {
                    fetchPedidos(); 
                }, 500);

                setSeleccionados(new Set());
            } else {
                alert("❌ Error enviando mensajes");
            }

        } catch (error) {
            console.error(error);
            alert("Error de conexión");
        } finally {
            setLoading(false);
        }
    };

    const formatMoney = (amount) => new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(amount);

    return (
    <div className="table-container fade-in">
    
      {/* BARRA SUPERIOR DE FILTROS */}
        <div className="toolbar-top">
        <div className="filter-group">
            <label>Estado Notificación:</label>
            <div className="btn-group">
                <button 
                    className={`btn-filter ${filtros.estadoNotificacion === 'TODOS' ? 'active' : ''}`}
                    onClick={() => setFiltros({...filtros, estadoNotificacion: 'TODOS'})}
                >Todos</button>
                <button 
                    className={`btn-filter pending ${filtros.estadoNotificacion === 'PENDIENTE' ? 'active' : ''}`}
                    onClick={() => setFiltros({...filtros, estadoNotificacion: 'PENDIENTE'})}
                >⏳ Pendientes</button>
                <button 
                    className={`btn-filter confirmed ${filtros.estadoNotificacion === 'CONFIRMADO' ? 'active' : ''}`}
                    onClick={() => setFiltros({...filtros, estadoNotificacion: 'CONFIRMADO'})}
                >✅ Confirmados</button>
                <button 
                    className={`btn-filter rejected ${filtros.estadoNotificacion === 'RECHAZADO' ? 'active' : ''}`}
                    onClick={() => setFiltros({...filtros, estadoNotificacion: 'RECHAZADO'})}
                >❌ Rechazados</button>
                {/* BOTÓN EXTRA: ENVIADOS */}
                <button 
                    className={`btn-filter sent ${filtros.estadoNotificacion === 'ENVIADO' ? 'active' : ''}`}
                    style={{backgroundColor: filtros.estadoNotificacion === 'ENVIADO' ? '#e0f2fe' : '', color: filtros.estadoNotificacion === 'ENVIADO' ? '#0369a1' : ''}}
                    onClick={() => setFiltros({...filtros, estadoNotificacion: 'ENVIADO'})}
                >🚀 Enviados</button>
            </div>
        </div>

        {/* BOTÓN DE ACCIÓN MASIVA */}
        <button 
            className="btn-whatsapp" 
            disabled={seleccionados.size === 0}
            onClick={enviarNotificaciones}
        >
            <MessageCircle size={18} /> Enviar WhatsApp ({seleccionados.size})
        </button>
        </div>

        <div className="toolbar">
        <div className="search-bar">
            <Search color="#999" size={20} />
            <input 
            type="text" 
            placeholder="Buscar..." 
            value={filtros.busquedaGlobal}
            onChange={(e) => setFiltros({...filtros, busquedaGlobal: e.target.value})}
            />
        </div>

        <div className="actions">
            <select 
                className="filter-select"
                value={filtros.estadoLogistico}
                onChange={(e) => setFiltros({...filtros, estadoLogistico: e.target.value})}
            >
                <option value="">Logística: Todos</option>
                <option value="ENTREGA_HOY">Entrega Hoy</option>
                <option value="EN_PISO">En Piso</option>
            </select>

            <button onClick={fetchPedidos} className="btn-icon" title="Recargar">
                <RefreshCw size={20} className={loading ? "spin" : ""} />
            </button>
            <button className="btn-export">
                <FileDown size={18} style={{marginRight:5}}/> XLS
            </button>
        </div>
        </div>

      {/* --- TABLA --- */}
        <div className="table-responsive">
        <table>
            <thead>
            <tr>
                <th style={{width: '40px', textAlign:'center'}}>
                    <input 
                        type="checkbox" 
                        onChange={handleSelectAll}
                        checked={pedidosFiltrados.length > 0 && seleccionados.size === pedidosFiltrados.length}
                        style={{cursor: 'pointer', width:'16px', height:'16px'}}
                    />
                </th>
                <th>Código / Entrega</th>
                <th>Cliente</th>
                <th>Contacto</th> {/* NUEVA COLUMNA */}
                <th>Estado Notif.</th> 
                <th>Fecha Carga</th> 
                <th>Conductor</th>
                <th>Valor</th>
                <th>Estado Log.</th>
            </tr>
            </thead>
        <tbody>
            {pedidosFiltrados.length > 0 ? (
                pedidosFiltrados.map((p) => {
                    const isSelected = seleccionados.has(p.id);
                    // Simulamos estado pendiente si viene null
                    const notifStatus = p.estadoNotificacion || 'PENDIENTE'; 

                    return (
                    <tr key={p.id} className={isSelected ? 'row-selected' : ''}>
                        <td style={{textAlign:'center'}}>
                            <input 
                                type="checkbox" 
                                checked={isSelected}
                                onChange={() => handleSelectOne(p.id)}
                                style={{cursor: 'pointer', width:'16px', height:'16px'}}
                            />
                        </td>
                        <td>
                            <div className="cell-code">
                                <span className="code-badge">{p.numeroEntrega}</span>
                                <small style={{color:'#666'}}>{p.numeroTransporte}</small>
                            </div>
                        </td>
                        <td>
                            <div className="cell-info">
                                <MapPin size={14} color="#D92D20" />
                                <strong>{p.cliente?.nombre || 'Desconocido'}</strong>
                            </div>
                        </td>

                        {/* --- NUEVA CELDA: TELÉFONO DEL CLIENTE --- */}
                        <td>
                            <div style={{display:'flex', alignItems:'center', gap:'5px', color:'#555'}}>
                                <Phone size={14} color="#16a34a"/>
                                <span style={{fontSize:'0.9rem'}}>{p.cliente?.telefono || 'N/A'}</span>
                            </div>
                        </td>
                        
                        {/* CELDA DE ESTADO NOTIFICACIÓN */}
                        <td>
                            <span className={`badge-notif ${notifStatus}`}>
                                {notifStatus === 'CONFIRMADO' && '✅ Confirmado'}
                                {notifStatus === 'RECHAZADO' && '❌ Rechazado'}
                                {notifStatus === 'PENDIENTE' && '⏳ Pendiente'}
                                {notifStatus === 'ENVIADO' && '🚀 Enviado'}
                            </span>
                        </td>

                        <td style={{fontSize:'0.85rem', color:'#666'}}>
                                {p.fechaCarga ? new Date(p.fechaCarga).toLocaleString('es-CO', {
                                day: '2-digit', month: '2-digit', hour: '2-digit', minute:'2-digit'
                                    }) : '-'}
                        </td>

                        <td>
                            <div className="cell-driver">
                            <div className="driver-row">
                                <User size={14} /> {p.nombreConductor}
                            </div>
                            <div className="plate-badge">
                                <Truck size={12} /> {p.placaVehiculo}
                            </div>
                            </div>
                        </td>
                        <td className="cell-money">
                            {formatMoney(p.valorTotal)}
                        </td>
                        <td>
                            <span className={`status-badge ${p.estadoLogistico}`}>
                            {p.estadoLogistico?.replace('_', ' ')}
                            </span>
                        </td>
                    </tr>
                )})
            ) : (
            <tr>
                <td colSpan="9" className="empty-state">No hay pedidos con estos filtros.</td>
            </tr>
            )}
        </tbody>
        </table>
        </div>
    
        <div className="table-footer">
        Mostrando <strong>{pedidosFiltrados.length}</strong> registros. Seleccionados: <strong>{seleccionados.size}</strong>
        </div>
    </div>
    );
}