from telethon import TelegramClient, events
from telethon.tl.functions.channels import JoinChannelRequest

import sys
import asyncio


async def on_event(e):
    print(e)
    pass


if __name__ == '__main__':
    token = sys.argv[1]
    token_split = token.split(':')
    api_id = token_split[0]
    api_hash = token_split[1]

    client = TelegramClient('main_session', int(api_id), api_hash)
    client.start()

    for channel in ['t.me/aviadispet4er', 't.me/fighter_bomber', 't.me/zonttest']:
        # noinspection PyTypeChecker
        entity = client.get_entity(channel)
        client(JoinChannelRequest(entity))

    # noinspection PyTypeChecker
    client.add_event_handler(on_event, events.NewMessage)
