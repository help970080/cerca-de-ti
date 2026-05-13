<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="24dp"
    tools:context=".ui.MainActivity">

    <ImageView
        android:id="@+id/ivLogo"
        android:layout_width="96dp"
        android:layout_height="96dp"
        android:layout_marginTop="48dp"
        android:contentDescription="@string/app_name"
        android:src="@drawable/ic_shield"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:tint="@color/primario" />

    <TextView
        android:id="@+id/tvTitulo"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:text="@string/app_name"
        android:textColor="@color/texto_principal"
        android:textSize="32sp"
        android:textStyle="bold"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/ivLogo" />

    <TextView
        android:id="@+id/tvEstado"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="48dp"
        android:gravity="center"
        android:padding="16dp"
        android:textSize="18sp"
        android:textColor="@color/texto_principal"
        tools:text="○ Inactivo. Toca el botón para activar."
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/tvTitulo" />

    <Button
        android:id="@+id/btnActivar"
        android:layout_width="match_parent"
        android:layout_height="64dp"
        android:layout_marginTop="32dp"
        android:backgroundTint="@color/acento"
        android:textColor="@color/white"
        android:textSize="18sp"
        android:textStyle="bold"
        android:text="@string/boton_activar"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/tvEstado" />

    <Button
        android:id="@+id/btnContactos"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="24dp"
        android:text="@string/boton_contactos"
        style="@style/Widget.MaterialComponents.Button.OutlinedButton"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/btnActivar" />

    <Button
        android:id="@+id/btnHistorial"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:text="@string/boton_historial"
        style="@style/Widget.MaterialComponents.Button.OutlinedButton"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/btnContactos" />

    <Button
        android:id="@+id/btnPermisos"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:text="@string/boton_permisos"
        style="@style/Widget.MaterialComponents.Button.OutlinedButton"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/btnHistorial" />

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="16dp"
        android:gravity="center"
        android:text="Proyecto sin fines de lucro · Código abierto MIT"
        android:textColor="@color/texto_secundario"
        android:textSize="12sp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
