$("#cp_items").change(function() {
	var cp = $(this).val().toString().split(';')[0];
	$.getJSON("${contextPath}/manager/ajax/getTransactionIds?chargeBoxId=" + cp, function(data) {	
		var options = "";
		$.each(data, function() {
			options += "<option value='" + this + "'>" + this + "</option>";
		});
		var select = $("#transactionId");
		select.prop("disabled", false);
		select.html(options);
	});
});