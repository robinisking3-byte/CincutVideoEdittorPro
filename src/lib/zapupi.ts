export const ZAP_UPI_CONFIG = {
  apiKey: 'zapfe5c7f3c3967c502e85648e908153d54',
  createOrderEndpoint: 'https://pay.zapupi.com/api/create-order',
  checkStatusEndpoint: 'https://pay.zapupi.com/api/order-status'
};

export interface ZapUpiCreateOrderResponse {
  status: string;
  message?: string;
  order_id: string;
  txn_id?: string;
  environment?: string;
  payment_url?: string;
  upi_button?: string;
  paytm_button?: string;
  payment_data?: string;
  payment_image_url?: string;
  auto_check_every_2_sec?: string;
  utr_check?: string;
  vpa_payment?: string;
}

export interface ZapUpiStatusResponse {
  status: string;
  message?: string;
  order_id?: string;
  txn_id?: string;
  amount?: string | number;
  utr?: string;
  date?: string;
  result?: string;
}

/**
 * Creates a real live ZapUPI payment order using the official merchant API key.
 */
export async function createRealZapUpiOrder(params: {
  orderId: string;
  amount: number;
  customerMobile?: string;
  remark?: string;
}): Promise<ZapUpiCreateOrderResponse> {
  const payload = {
    zap_key: ZAP_UPI_CONFIG.apiKey,
    order_id: params.orderId,
    amount: params.amount.toFixed(2),
    customer_mobile: params.customerMobile || '9876543210',
    remark: params.remark || 'CineCut Studios VIP Subscription'
  };

  const response = await fetch(ZAP_UPI_CONFIG.createOrderEndpoint, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    },
    body: JSON.stringify(payload)
  });

  if (!response.ok) {
    throw new Error(`ZapUPI gateway error (HTTP ${response.status})`);
  }

  const data: ZapUpiCreateOrderResponse = await response.json();
  if (data.status !== 'success') {
    throw new Error(data.message || 'Failed to create order on ZapUPI gateway');
  }

  return data;
}

/**
 * Polls or checks the live status of an order on the ZapUPI gateway.
 */
export async function checkRealZapUpiOrderStatus(orderId: string): Promise<ZapUpiStatusResponse> {
  const payload = {
    zap_key: ZAP_UPI_CONFIG.apiKey,
    order_id: orderId
  };

  try {
    const response = await fetch(ZAP_UPI_CONFIG.checkStatusEndpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      return { status: 'pending', message: 'Payment awaiting confirmation' };
    }

    const data = await response.json();
    return data;
  } catch (err: any) {
    return { status: 'pending', message: err.message || 'Status check pending' };
  }
}
