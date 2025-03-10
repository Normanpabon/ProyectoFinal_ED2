package clouddrive

import grails.validation.ValidationException
import static org.springframework.http.HttpStatus.*

class UsuarioController {

    UsuarioService usuarioService
    ArchivoController archivoController = new ArchivoController()
    

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    static defaultAction = "dirigirLogin"


    def deshabilitarCuenta(){

        verificarUsuarioLogueado()
        
        def tmpUser = Usuario.get(session.user)
        
        if(tmpUser != null ){

            if(tmpUser.hashed_pass == (params.claveOld+tmpUser.nombre_usuario).digest('SHA-256') ){
                tmpUser.activo = 0
                try{
                    usuarioService.save(tmpUser)
                    flash.message = "Cuenta desactivada correctamente"
                    redirect(action: "dirigirLogin")
                }catch(ValidationException e){
                    
                }
            }else{
                
                flash.message = "La clave es incorrecta"
                redirect(action: "dirigirPerfil")
            }
        
        
        
        }else{
            
            redirect(action: "dirigirLogin")
        }


        // logic : debe asignar el parametro de la cuenta dada como un 0 para que se encuentre deshabilitada
    }

    def registrarUsuario(){

        

        // aplica sha-256 a la clave, se usa el nombre de usuario como salt
        params.hashed_pass = params.hashed_pass+params.usuario
        params.hashed_pass = params.hashed_pass.digest('SHA-256') 
        
        def tmpUser = new Usuario(nombre_usuario: params.usuario, correo:params.correo, hashed_pass:params.hashed_pass, activo:1, cuota:5120)
        //def tmpUser = new Usuario(params)
        if(tmpUser != null && Usuario.findByNombre_usuario(params.usuario) == null){
            try{
                usuarioService.save(tmpUser)
                // muestra msg temporal
                flash.message = 'Usuario' + params.usuario +'creado correctamente!' 
                redirect(action: "dirigirLogin")

                //notificar al usuario creacion exitosa ¿Redirigir al perfil ? 

                // todo : redirigir al perfil o archivos
            }catch(ValidationException e){
                //notificar al usuario
                render "error al crear usuario"
            }
        }else{
            flash.error = ' ERROR el usuario ' + params.usuario +' ya esta registrado!'
            redirect(action: "dirigirRegistro") 
        }

        
    }

    def modificarContra(){
        verificarUsuarioLogueado()
        
        // encontrar usuario con id del cookie. si no esta logueado, la cookie con id de usuario no estaria inicializada
        def tmpUser = Usuario.get(session.user)

                
        // se espera params.viejaContra y params.nuevaContra
        if(tmpUser != null ){


            if(tmpUser.hashed_pass == (params.viejaContra+tmpUser.nombre_usuario).digest('SHA-256')){
                
                tmpUser.hashed_pass = (params.nuevaContra+tmpUser.nombre_usuario).digest('SHA-256')

                try {
                    usuarioService.save(tmpUser)
                } catch (ValidationException e) {
                    respond usuario.errors
                    return
                }
                
                flash.message = "Contraseña cambiada correctamente"
                redirect(action: "dirigirPerfil")

            }else{
                flash.message = "La contraseña anterior es incorrecta"
                redirect(action: "dirigirPerfil")

                //todo notificar al usuario de error en la contraseña anterior
            }
        }else{
            redirect(action: "dirigirLogin")
        }
        
    }

    def dirigirHome(){
        verificarUsuarioLogueado()
        archivoController.listarArchivos()
        render(view: "home")
    }

    def dirigirLogin(){
        
        render(view: "loginTest")
    }

    def dirigirRegistro(){
        render(view:"registroTest") // esta bugueada esa madre
    }

    def dirigirPerfil(){
        verificarUsuarioLogueado()
        render(view:"perfil")
    }

    def dirigirArchivosCompartidos(){
        verificarUsuarioLogueado()
        archivoController.listarArchivosCompartidosMe()
        render(view:"compartido")
    
    }

    def verificarUsuarioLogueado(){

        def tmpUser = Usuario.get(session.user)

        if(tmpUser == null){
            redirect(action: "dirigirLogin")
        }else{
            if(tmpUser.activo == 0){
                redirect(action: "dirigirLogin")
            }
        }

    }

    def loguearUsuario(){
        
        //GORM querie

        def tmpUser = Usuario.findByNombre_usuario(params.usuario)

        // aplica sha-256 a la clave, se usa el nombre de usuario como salt
        params.hashed_pass = params.hashed_pass+params.usuario
        params.hashed_pass = params.hashed_pass.digest('SHA-256') 
       
       
        if(tmpUser != null){

            
            
            if(tmpUser.getHashed_pass() == params.hashed_pass && tmpUser.activo == 1){
                
                //logueo exitoso
                setUsuarioActual(tmpUser.getId())

                //render "Usuario encontrado, logueado correctamente. DEBUG params: " + params + "\nDEBUG SESSION: " + session

                redirect(action: "dirigirHome") 

                //redirigir a perfil

            }else{

                
                flash.message = " Usuario o contraseña incorrectos"
                redirect(action: "dirigirLogin")  
                // mostrar usuario o contraseña incorrectos
            }
        }else{
            
            flash.message = " Usuario o contraseña incorrectos"
            redirect(action: "dirigirLogin")  
            // mostrar usuario o contraseña incorrectos
        }
        

        //llamar a setUsuarioActual si este se autentica correctamente
    }

    def setUsuarioActual(int id){
        // colocar id del usuario o nombre de usuario en la cookie para autenticarlo
        session["user"] = id
        //verificar funcionamiento
        //render "Añadido usuario $tmpUser a la session" // borrar

    }

    def cerrarSesion(){
        
        // revocar cookie

        session.invalidate()
        redirect(action: "dirigirLogin")  

    }


    
    def modCorreo(){
        
        verificarUsuarioLogueado()
        //logic
        def tmpUser = Usuario.get(session.user)
        
        // se espera params.correoNuevo 
        
        if(tmpUser != null){
            tmpUser.correo = params.correoNuevo
            
            try {
                    usuarioService.save(tmpUser)
                } catch (ValidationException e) {
                    respond usuario.errors
                    return
                }
            flash.message = "Correo cambiado correctamente"
            redirect(action: "dirigirPerfil")
        
        }else{
            redirect(action: "dirigirLogin")
        }

    }

    def verPerfilUsuario(){
        //logic
        def tmpUser = usuarioService.get(session.user)

        // aca seria tener una vista de perfil y pasarle de parametro el id del usuario a renderizar los datos o el obj temporal "tmpUser"
    }

    
}
