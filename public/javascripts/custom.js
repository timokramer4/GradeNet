"use strict"

// Clickable table rows
$(".clickable").click(function (event) {
    event.stopPropagation();
    window.location = this.getAttribute("data-href");
});

// Data tables
$(document).ready(function () {
    $('.dataTable').DataTable();
});

// Show current selected file
$('body').on('change', '.custom-file-input', function () {
    console.log("Change");

    // Get filename
    var fileName = $(this).val().replace(/^.*[\\\/]/, '');

    // Replace default string
    if (fileName == "") {
        $(this).parent().parent().next('.custom-file-label')[0].innerText = "Datei auswählen";
    } else {
        $(this).parent().parent().next('.custom-file-label')[0].innerText = fileName;
    }
});

// Close alert after 5 seconds
$('.alert').each(function (index, element) {
    var $element = $(element),
        timeout  = 5000;

    setTimeout(function () {
        $element.alert('close');
    }, timeout);
});